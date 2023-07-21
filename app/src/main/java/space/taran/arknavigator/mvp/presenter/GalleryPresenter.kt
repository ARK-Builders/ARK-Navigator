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
import space.taran.arklib.domain.index.Resource
import space.taran.arklib.domain.index.ResourceIndex
import space.taran.arklib.domain.index.ResourceIndexRepo
import space.taran.arklib.domain.meta.Kind
import space.taran.arklib.domain.meta.Metadata
import space.taran.arklib.domain.meta.MetadataProcessor
import space.taran.arklib.domain.meta.MetadataProcessorRepo
import space.taran.arklib.domain.preview.PreviewLocator
import space.taran.arklib.domain.preview.PreviewProcessor
import space.taran.arklib.domain.preview.PreviewProcessorRepo
import space.taran.arklib.domain.storage.StorageException
import space.taran.arklib.domain.score.ScoreStorage
import space.taran.arklib.domain.score.ScoreStorageRepo
import space.taran.arklib.domain.stats.StatsEvent
import space.taran.arklib.domain.tags.TagStorage
import space.taran.arklib.domain.tags.Tags
import space.taran.arklib.domain.tags.TagsStorageRepo
import space.taran.arklib.utils.ImageUtils
import space.taran.arklib.utils.extension
import space.taran.arknavigator.di.modules.RepoModule.Companion.MESSAGE_FLOW_NAME
import space.taran.arknavigator.mvp.model.repo.preferences.PreferenceKey
import space.taran.arknavigator.mvp.model.repo.preferences.Preferences
import space.taran.arknavigator.mvp.model.repo.stats.StatsStorage
import space.taran.arknavigator.mvp.model.repo.stats.StatsStorageRepo
import space.taran.arknavigator.mvp.presenter.adapter.ResourceDiffUtilCallback
import space.taran.arknavigator.mvp.view.GalleryView
import space.taran.arknavigator.navigation.AppRouter
import space.taran.arknavigator.navigation.Screens
import space.taran.arknavigator.ui.adapter.previewpager.PreviewItemView
import space.taran.arknavigator.ui.adapter.previewpager.PreviewPlainTextItemView
import space.taran.arknavigator.utils.LogTags.GALLERY_SCREEN
import space.taran.arknavigator.utils.Score
import space.taran.arknavigator.utils.Tag
import java.io.FileNotFoundException
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Named
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.notExists

class GalleryPresenter(
    private val rootAndFav: RootAndFav,
    private val resourcesIds: List<ResourceId>,
    private val startAt: Int,
    var selectingEnabled: Boolean,
    private val selectedResources: MutableList<ResourceId>
) : MvpPresenter<GalleryView>() {

    lateinit var index: ResourceIndex
        private set
    lateinit var tagsStorage: TagStorage
        private set
    lateinit var previewStorage: PreviewProcessor
        private set
    lateinit var metadataStorage: MetadataProcessor
        private set
    lateinit var statsStorage: StatsStorage
        private set
    lateinit var scoreStorage: ScoreStorage
        private set

    data class GalleryItem(
        val resource: Resource,
        val preview: PreviewLocator,
        val metadata: Metadata
    ) {
        fun id() = resource.id
    }

    var galleryItems: MutableList<GalleryItem> = mutableListOf()
        private set

    var diffResult: DiffUtil.DiffResult? = null
        private set

    private var currentPos = startAt

    private val currentItem: GalleryItem
        get() = galleryItems[currentPos]

    private var sortByScores = false
    private var isControlsVisible = false

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var router: AppRouter

    @Inject
    lateinit var indexRepo: ResourceIndexRepo

    @Inject
    lateinit var previewStorageRepo: PreviewProcessorRepo

    @Inject
    lateinit var metadataStorageRepo: MetadataProcessorRepo

    @Inject
    lateinit var tagsStorageRepo: TagsStorageRepo

    @Inject
    lateinit var statsStorageRepo: StatsStorageRepo

    @Inject
    lateinit var scoreStorageRepo: ScoreStorageRepo

    @Inject
    @Named(MESSAGE_FLOW_NAME)
    lateinit var messageFlow: MutableSharedFlow<Message>

    override fun onFirstViewAttach() {
        Log.d(GALLERY_SCREEN, "first view attached in GalleryPresenter")
        super.onFirstViewAttach()
        presenterScope.launch {
            viewState.init()
            viewState.setProgressVisibility(true, "Providing root index")

            index = indexRepo.provide(rootAndFav)
            messageFlow.onEach { message ->
                when (message) {
                    is Message.KindDetectFailed -> viewState.toastIndexFailedPath(
                        message.path
                    )
                }
            }.launchIn(presenterScope)

            viewState.setProgressVisibility(true, "Providing metadata storage")
            metadataStorage = metadataStorageRepo.provide(index)

            viewState.setProgressVisibility(true, "Providing previews storage")
            previewStorage = previewStorageRepo.provide(index)

            viewState.setProgressVisibility(true, "Proviging data storages")

            try {
                tagsStorage = tagsStorageRepo.provide(index)
                scoreStorage = scoreStorageRepo.provide(index)
            } catch (e: StorageException) {
                viewState.displayStorageException(
                    e.label,
                    e.msg
                )
            }

            statsStorage = statsStorageRepo.provide(index)

            galleryItems = resourcesIds.map { id ->
                val preview = previewStorage.retrieve(id).getOrThrow()
                val metadata = metadataStorage.retrieve(id).getOrThrow()

                val resource = index.getResource(id)!!
                GalleryItem(resource, preview, metadata)
            }.toMutableList()

            sortByScores = preferences.get(PreferenceKey.SortByScores)

            viewState.updatePagerAdapter()
            viewState.setProgressVisibility(false)
            viewState.setScoringControlsVisibility(sortByScores)
        }
    }

    fun onPageChanged(newPos: Int) = presenterScope.launch {
        if (galleryItems.isEmpty())
            return@launch

        checkResourceChanges(newPos)

        currentPos = newPos

        val id = currentItem.id()
        val tags = tagsStorage.getTags(id)
        val score = scoreStorage.getScore(id)
        displayPreview(id, currentItem.metadata, tags, score)
    }

    fun onResume() {
        checkResourceChanges(currentPos)
    }

    fun getKind(pos: Int): Kind =
        galleryItems[pos].metadata.kind

    fun bindView(view: PreviewItemView) = presenterScope.launch {
        view.reset()
        val item = galleryItems[view.pos]

        val path = index.getPath(item.id())!!
        val placeholder = ImageUtils.iconForExtension(extension(path))
        view.setSource(placeholder, item.id(), item.metadata, item.preview)
    }

    fun bindPlainTextView(view: PreviewPlainTextItemView) = presenterScope.launch {
        view.reset()
        val item = galleryItems[view.pos]

        val path = index.getPath(item.id())!!
        val content = readText(path)

        content.onSuccess {
            view.setContent(it)
        }
    }

    fun onTagsChanged() {
        val tags = tagsStorage.getTags(currentItem.id())
        viewState.displayPreviewTags(currentItem.id(), tags)
    }

    fun onOpenFabClick() = presenterScope.launch {
        Log.d(GALLERY_SCREEN, "[open_resource] clicked at position $currentPos")

        val id = currentItem.id()
        val path = index.getPath(id)!!

        if (currentItem.metadata is Metadata.Link) {
            val url = readText(path).getOrThrow()
            viewState.openLink(url)

            return@launch
        }

        viewState.viewInExternalApp(path)
    }

    fun onInfoFabClick() = presenterScope.launch {
        Log.d(GALLERY_SCREEN, "[info_resource] clicked at position $currentPos")

        val path = index.getPath(currentItem.id())!!
        viewState.showInfoAlert(path, currentItem.resource, currentItem.metadata)
    }

    fun onShareFabClick() = presenterScope.launch {
        Log.d(GALLERY_SCREEN, "[share_resource] clicked at position $currentPos")
        val path = index.getPath(currentItem.id())!!

        if (currentItem.metadata is Metadata.Link) {
            val url = readText(path).getOrThrow()
            viewState.shareLink(url)
            return@launch
        }

        viewState.shareResource(path)
    }

    fun onEditFabClick() = presenterScope.launch {
        Log.d(GALLERY_SCREEN, "[edit_resource] clicked at position $currentPos")
        val path = index.getPath(currentItem.id())!!
        viewState.editResource(path)
    }

    fun onRemoveFabClick() = presenterScope.launch(NonCancellable) {
        Log.d(GALLERY_SCREEN, "[remove_resource] clicked at position $currentPos")
        deleteResource(currentItem.id())
        galleryItems.removeAt(currentPos)

        if (galleryItems.isEmpty()) {
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
        val id = currentItem.id()

        val tags = tagsStorage.getTags(id)
        val newTags = tags - tag

        viewState.displayPreviewTags(id, newTags)
        statsStorage.handleEvent(
            StatsEvent.TagsChanged(
                id, tags, newTags
            )
        )

        Log.d(GALLERY_SCREEN, "setting new tags $newTags to $currentItem")

        tagsStorage.setTags(id, newTags)
        tagsStorage.persist()
        viewState.notifyTagsChanged()
    }

    fun onSelectBtnClick() {
        val id = currentItem.id()
        val wasSelected = id in selectedResources

        if (wasSelected) {
            selectedResources.remove(id)
        } else {
            selectedResources.add(id)
        }

        viewState.displaySelected(
            !wasSelected,
            showAnim = true,
            selectedResources.size,
            galleryItems.size
        )
    }

    fun onEditTagsDialogBtnClick() {
        viewState.showEditTagsDialog(currentItem.id())
    }

    fun onIncreaseScore() = changeScore(1)

    fun onDecreaseScore() = changeScore(-1)

    private fun changeScore(inc: Score) =
        presenterScope.launch {
            val id = currentItem.id()
            val score = scoreStorage.getScore(id) + inc

            scoreStorage.setScore(id, score)
            withContext(Dispatchers.IO) {
                scoreStorage.persist()
            }
            withContext(Dispatchers.Main) {
                viewState.displayScore(score)
            }
            viewState.notifyResourceScoresChanged()
        }

    private fun checkResourceChanges(pos: Int) =
        presenterScope.launch(Dispatchers.IO) {
            if (galleryItems.isEmpty()) {
                return@launch
            }

            val item = galleryItems[pos]

            val path = index.getPath(item.id())
                ?: throw IllegalStateException(
                    "Resource ${item.id()} can't be found in the index"
                )

            if (path.notExists()) {
                throw FileNotFoundException(
                    "Resource ${item.id()} isn't stored by path $path"
                )
            }
            if (path.getLastModifiedTime() != item.resource.modified) {
                throw IllegalStateException(
                    "Index is not up-to-date regarding path $path"
                )
            }
        }

    private suspend fun deleteResource(resource: ResourceId) {
        Log.d(GALLERY_SCREEN, "deleting resource $resource")

        val path = index.getPath(resource)

        Files.delete(path)
        tagsStorage.remove(resource)

        index.updateAll()
        viewState.notifyResourcesChanged()
    }

    private fun displayPreview(
        id: ResourceId,
        meta: Metadata,
        tags: Tags,
        score: Score
    ) {
        viewState.setupPreview(currentPos, meta)
        viewState.displayPreviewTags(id, tags)
        viewState.displayScore(score)
        viewState.displaySelected(
            id in selectedResources,
            showAnim = false,
            selectedResources.size,
            galleryItems.size
        )
    }

    private suspend fun invalidateResources() {
        val newItems = galleryItems.filter { item ->
            index.allIds().contains(item.resource.id)
        }.toMutableList()

        if (newItems.isEmpty()) {
            onBackClick()
            return
        }

        diffResult = DiffUtil.calculateDiff(
            ResourceDiffUtilCallback(
                galleryItems.map { it.resource.id },
                newItems.map { it.resource.id }
            )
        )
        galleryItems = newItems

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
            selectedResources.add(currentItem.resource.id)
        }
    }

    fun onPlayButtonClick() = presenterScope.launch {
        viewState.viewInExternalApp(index.getPath(currentItem.id())!!)
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
