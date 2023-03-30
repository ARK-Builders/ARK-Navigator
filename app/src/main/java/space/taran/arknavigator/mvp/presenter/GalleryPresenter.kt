package space.taran.arknavigator.mvp.presenter

import android.util.Log
import androidx.recyclerview.widget.DiffUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.MvpPresenter
import moxy.presenterScope
import space.taran.arkfilepicker.folders.RootAndFav
import space.taran.arklib.ResourceId
import space.taran.arklib.domain.Message
import space.taran.arklib.domain.index.ResourceMeta
import space.taran.arklib.domain.index.ResourcesIndex
import space.taran.arklib.domain.index.ResourcesIndexRepo
import space.taran.arklib.domain.kind.ResourceKind
import space.taran.arklib.domain.meta.MetadataStorage
import space.taran.arklib.domain.meta.MetadataStorageRepo
import space.taran.arklib.domain.preview.PreviewStorage
import space.taran.arklib.domain.preview.PreviewStorageRepo
import space.taran.arklib.utils.Constants
import space.taran.arknavigator.mvp.model.repo.preferences.PreferenceKey
import space.taran.arknavigator.mvp.model.repo.preferences.Preferences
import space.taran.arknavigator.mvp.model.repo.scores.ScoreStorage
import space.taran.arknavigator.mvp.model.repo.scores.ScoreStorageRepo
import space.taran.arknavigator.mvp.model.repo.stats.StatsStorage
import space.taran.arknavigator.mvp.model.repo.stats.StatsStorageRepo
import space.taran.arknavigator.mvp.model.repo.tags.PlainTagsStorage
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorage
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorageRepo
import space.taran.arknavigator.mvp.presenter.adapter.ResourceMetaDiffUtilCallback
import space.taran.arknavigator.mvp.view.GalleryView
import space.taran.arknavigator.navigation.AppRouter
import space.taran.arknavigator.navigation.Screens
import space.taran.arknavigator.ui.adapter.previewpager.PreviewItemView
import space.taran.arknavigator.ui.adapter.previewpager.PreviewPlainTextItemView
import space.taran.arknavigator.utils.LogTags.GALLERY_SCREEN
import space.taran.arknavigator.utils.Score
import space.taran.arknavigator.utils.Tag
import timber.log.Timber
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Named
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.notExists

enum class GalleryItemType {
    PLAINTEXT, OTHER
}

class GalleryPresenter(
    private val rootAndFav: RootAndFav,
    private val resourcesIds: List<ResourceId>,
    private val startAt: Int,
    var selectingEnabled: Boolean,
    private val selectedResources: MutableList<ResourceId>
) : MvpPresenter<GalleryView>() {

    private var isControlsVisible = false
    private var currentPos = startAt
    private val currentResource: ResourceMeta
        get() = resources[currentPos]

    lateinit var index: ResourcesIndex
        private set
    lateinit var storage: TagsStorage
        private set
    lateinit var previewStorage: PreviewStorage
        private set
    lateinit var metadataStorage: MetadataStorage
        private set
    lateinit var statsStorage: StatsStorage
        private set
    lateinit var scoreStorage: ScoreStorage
        private set
    var resources: MutableList<ResourceMeta> = mutableListOf()
        private set
    var diffResult: DiffUtil.DiffResult? = null
        private set
    var sortByScores = false

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var router: AppRouter

    @Inject
    lateinit var indexRepo: ResourcesIndexRepo

    @Inject
    lateinit var previewStorageRepo: PreviewStorageRepo

    @Inject
    lateinit var metadataStorageRepo: MetadataStorageRepo

    @Inject
    lateinit var tagsStorageRepo: TagsStorageRepo

    @Inject
    lateinit var statsStorageRepo: StatsStorageRepo

    @Inject
    lateinit var scoreStorageRepo: ScoreStorageRepo

    @Inject
    @Named(Constants.DI.MESSAGE_FLOW_NAME)
    lateinit var messageFlow: MutableSharedFlow<Message>

    override fun onFirstViewAttach() {
        Log.d(GALLERY_SCREEN, "first view attached in GalleryPresenter")
        super.onFirstViewAttach()
        presenterScope.launch {
            viewState.init()
            viewState.setProgressVisibility(true, "Indexing")

            index = indexRepo.provide(rootAndFav)
            messageFlow.onEach { message ->
                when (message) {
                    is Message.KindDetectFailed -> viewState.toastIndexFailedPath(
                        message.path
                    )
                }
            }.launchIn(presenterScope)
            storage = tagsStorageRepo.provide(rootAndFav)
            previewStorage = previewStorageRepo.provide(rootAndFav)
            metadataStorage = metadataStorageRepo.provide(rootAndFav)
            statsStorage = statsStorageRepo.provide(rootAndFav)
            scoreStorage = scoreStorageRepo.provide(rootAndFav)

            if (storage.isCorrupted()) viewState.showCorruptNotificationDialog(
                PlainTagsStorage.TYPE
            )

            resources = resourcesIds.map { index.getMeta(it) }.toMutableList()
            sortByScores = preferences.get(PreferenceKey.SortByScores)

            viewState.updatePagerAdapter()
            viewState.setProgressVisibility(false)
            viewState.setScoringControlsVisibility(sortByScores)
        }
    }

    fun onPageChanged(newPos: Int) = presenterScope.launch {
        if (resources.isEmpty())
            return@launch

        checkResourceChanges(newPos)

        currentPos = newPos
        displayPreview()
    }

    fun onResume() {
        checkResourceChanges(currentPos)
    }

    fun detectItemType(pos: Int) = when (resources[pos].kind) {
        is ResourceKind.PlainText -> GalleryItemType.PLAINTEXT
        else -> GalleryItemType.OTHER
    }

    fun bindView(view: PreviewItemView) = presenterScope.launch {
        view.reset()
        val meta = resources[view.pos]
        val path = index.getPath(meta.id)
        view.setSource(path, meta, previewStorage.locate(path, meta))
    }

    fun bindPlainTextView(view: PreviewPlainTextItemView) = presenterScope.launch {
        view.reset()
        val meta = resources[view.pos]
        val path = index.getPath(meta.id)
        val contentResult = readText(path)
        contentResult.onSuccess {
            view.setContent(it)
        }
    }

    fun onTagsChanged() {
        val tags = storage.getTags(currentResource.id)
        viewState.displayPreviewTags(currentResource.id, tags)
    }

    fun onOpenFabClick() = presenterScope.launch {
        Log.d(GALLERY_SCREEN, "[open_resource] clicked at position $currentPos")
        val kind = currentResource.kind
        if (kind is ResourceKind.Link) {
            val url = kind.url ?: return@launch
            viewState.openLink(url)
            return@launch
        }

        viewState.viewInExternalApp(index.getPath(currentResource.id))
    }

    fun onInfoFabClick() = presenterScope.launch {
        Log.d(GALLERY_SCREEN, "[info_resource] clicked at position $currentPos")
        viewState.showInfoAlert(index.getPath(currentResource.id), currentResource)
    }

    fun onShareFabClick() = presenterScope.launch {
        Log.d(GALLERY_SCREEN, "[share_resource] clicked at position $currentPos")
        val kind = currentResource.kind
        if (kind is ResourceKind.Link) {
            val url = kind.url ?: return@launch
            viewState.shareLink(url)
            return@launch
        }

        viewState.shareResource(index.getPath(currentResource.id))
    }

    fun onEditFabClick() = presenterScope.launch {
        Log.d(GALLERY_SCREEN, "[edit_resource] clicked at position $currentPos")
        viewState.editResource(index.getPath(currentResource.id))
    }

    fun onRemoveFabClick() = presenterScope.launch(NonCancellable) {
        Log.d(GALLERY_SCREEN, "[remove_resource] clicked at position $currentPos")
        deleteResource(currentResource.id)
        resources.removeAt(currentPos)
        if (resources.isEmpty()) {
            onBackClick()
            return@launch
        }
        onTagsChanged()
        viewState.deleteResource(currentPos)
    }

    fun onTagSelected(tag: Tag) {
        router.navigateTo(
            Screens.ResourcesScreenWithSelectedTag(
                rootAndFav, tag
            )
        )
    }

    fun onTagRemove(tag: Tag) = presenterScope.launch(NonCancellable) {
        val id = currentResource.id
        val tags = storage.getTags(id)
        val newTags = tags - tag
        viewState.displayPreviewTags(id, newTags)
        Log.d(GALLERY_SCREEN, "tags $tags set to $currentResource")
        storage.setTagsAndPersist(currentResource.id, newTags)
        viewState.notifyTagsChanged()
    }

    fun onSelectBtnClick() {
        val wasSelected = currentResource.id in selectedResources

        if (wasSelected)
            selectedResources.remove(currentResource.id)
        else
            selectedResources.add(currentResource.id)

        viewState.displaySelected(
            !wasSelected,
            showAnim = true,
            selectedResources.size,
            resources.size
        )
    }

    fun onEditTagsDialogBtnClick() {
        viewState.showEditTagsDialog(currentResource.id)
    }

    fun onIncreaseScore() = changeScore(1)

    fun onDecreaseScore() = changeScore(-1)

    private fun changeScore(inc: Score) = presenterScope.launch {
        val score = scoreStorage.getScore(currentResource.id) + inc
        scoreStorage.setScore(currentResource.id, score)
        withContext(Dispatchers.IO) {
            scoreStorage.persist()
        }
        withContext(Dispatchers.Main) {
            viewState.displayScore(score)
        }
        viewState.notifyResourceScoresChanged()
    }

    private fun checkResourceChanges(
        pos: Int
    ) = presenterScope.launch(Dispatchers.IO) {
        if (resources.isEmpty()) return@launch
        val meta = resources[pos]

        val path = try {
            index.getPath(meta.id)
        } catch (e: Throwable) {
            Timber.e(
                "Resource[${meta.id}] is presented in gallery but not in index",
                e
            )
            onRemovedOrEditedResourceDetected()
            return@launch
        }

        if (path.notExists() || path.getLastModifiedTime() != meta.modified) {
            onRemovedOrEditedResourceDetected()
        }
    }

    private suspend fun deleteResource(resource: ResourceId) {
        Log.d(GALLERY_SCREEN, "deleting resource $resource")

        storage.remove(resource)
        val path = index.remove(resource)
        Log.d(GALLERY_SCREEN, "path $path removed from index")
        viewState.notifyResourcesChanged()

        presenterScope.launch(NonCancellable + Dispatchers.IO) {
            Files.delete(path)
        }
    }

    private suspend fun displayPreview() {
        val resource = resources[currentPos]
        val tags = storage.getTags(resource.id)
        val filePath = index.getPath(resource.id)
        val score = scoreStorage.getScore(resource.id)
        viewState.setupPreview(currentPos, resource, filePath.fileName.toString())
        viewState.displayPreviewTags(resource.id, tags)
        viewState.displayScore(score)
        viewState.displaySelected(
            resource.id in selectedResources,
            showAnim = false,
            selectedResources.size,
            resources.size
        )
    }

    private suspend fun onRemovedOrEditedResourceDetected() =
        withContext(Dispatchers.Main) {
            viewState.setProgressVisibility(true, "Indexing")

            index.reindex()
            // update current storages with new resources
            tagsStorageRepo.provide(rootAndFav)
            scoreStorageRepo.provide(rootAndFav)

            invalidateResources()
            viewState.notifyCurrentItemChanged()
            viewState.notifyResourcesChanged()
            viewState.setProgressVisibility(false)
        }

    private suspend fun invalidateResources() {
        val indexedIds = index.listIds(rootAndFav.fav)
        val newResources =
            resources.filter { meta -> indexedIds.contains(meta.id) }.toMutableList()

        if (newResources.isEmpty()) {
            onBackClick()
            return
        }

        diffResult = DiffUtil.calculateDiff(
            ResourceMetaDiffUtilCallback(
                resources,
                newResources
            )
        )
        resources = newResources

        viewState.updatePagerAdapterWithDiff()
    }

    fun onPreviewsItemClick() {
        isControlsVisible = !isControlsVisible
        viewState.setControlsVisibility(isControlsVisible)
    }

    fun onSelectingChanged() {
        selectingEnabled = !selectingEnabled
        viewState.toggleSelecting(selectingEnabled)
        selectedResources.clear()
        if (selectingEnabled) {
            selectedResources.add(currentResource.id)
        }
    }

    fun onPlayButtonClick() = presenterScope.launch {
        viewState.viewInExternalApp(index.getPath(currentResource.id))
    }

    fun onBackClick() {
        Log.d(GALLERY_SCREEN, "quitting from GalleryPresenter")
        viewState.notifySelectedChanged(selectedResources)
        viewState.exitFullscreen()
        router.exit()
    }

    private suspend fun readText(source: Path): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val content = FileReader(source.toFile()).readText()
                Result.success(content)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
