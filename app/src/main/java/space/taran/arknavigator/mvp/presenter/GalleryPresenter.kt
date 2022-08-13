package space.taran.arknavigator.mvp.presenter

import android.util.Log
import androidx.recyclerview.widget.DiffUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.MvpPresenter
import moxy.presenterScope
import space.taran.arknavigator.mvp.model.repo.RootAndFav
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndexRepo
import space.taran.arknavigator.mvp.model.repo.kind.ResourceKind
import space.taran.arknavigator.mvp.model.repo.preview.PreviewAndThumbnail
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorage
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorageRepo
import space.taran.arknavigator.mvp.presenter.adapter.ResourceMetaDiffUtilCallback
import space.taran.arknavigator.mvp.view.GalleryView
import space.taran.arknavigator.navigation.AppRouter
import space.taran.arknavigator.navigation.Screens
import space.taran.arknavigator.ui.adapter.previewpager.PreviewItemView
import space.taran.arknavigator.ui.adapter.previewpager.PreviewPlainTextItemView
import space.taran.arknavigator.utils.LogTags.GALLERY_SCREEN
import space.taran.arknavigator.utils.Tag
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.notExists

enum class GalleryItemType {
    PLAINTEXT, OTHER
}

class GalleryPresenter(
    private val rootAndFav: RootAndFav,
    private val resourcesIds: List<ResourceId>,
    private val startAt: Int
) : MvpPresenter<GalleryView>() {

    private var isControlsVisible = false
    private var currentPos = startAt
    private val currentResource: ResourceMeta
        get() = resources[currentPos]

    lateinit var index: ResourcesIndex
        private set
    lateinit var storage: TagsStorage
        private set
    var resources: MutableList<ResourceMeta> = mutableListOf()
        private set
    var diffResult: DiffUtil.DiffResult? = null
        private set

    @Inject
    lateinit var router: AppRouter

    @Inject
    lateinit var indexRepo: ResourcesIndexRepo

    @Inject
    lateinit var tagsStorageRepo: TagsStorageRepo

    override fun onFirstViewAttach() {
        Log.d(GALLERY_SCREEN, "first view attached in GalleryPresenter")
        super.onFirstViewAttach()
        presenterScope.launch {
            viewState.init()
            if (!indexRepo.isIndexed(rootAndFav))
                viewState.setProgressVisibility(true, "Indexing")

            index = indexRepo.provide(rootAndFav)
            index.kindDetectFailedFlow.onEach { failed ->
                viewState.toastIndexFailedPath(failed)
            }.launchIn(presenterScope)
            storage = tagsStorageRepo.provide(rootAndFav)
            resources = resourcesIds.map { index.getMeta(it) }.toMutableList()

            viewState.updatePagerAdapter()
            viewState.setProgressVisibility(false)
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
        view.setSource(path, meta)
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

    fun onEditTagsDialogBtnClick() {
        viewState.showEditTagsDialog(currentResource.id)
    }

    private fun checkResourceChanges(
        pos: Int
    ) = presenterScope.launch(Dispatchers.IO) {
        if (resources.isEmpty()) return@launch
        val meta = resources[pos]
        val path = index.getPath(meta.id)
        if (path.notExists()) {
            onRemovedResourceDetected()
            return@launch
        }
        if (path.getLastModifiedTime() != meta.modified)
            onEditedResourceDetected(path, meta)
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
        viewState.setupPreview(currentPos, resource, filePath.fileName.toString())
        viewState.displayPreviewTags(resource.id, tags)
    }

    private suspend fun onRemovedResourceDetected() = withContext(Dispatchers.Main) {
        viewState.setProgressVisibility(true, "Indexing")

        index.reindex()
        // update current tags storage
        tagsStorageRepo.provide(rootAndFav)
        invalidateResources()
        viewState.setProgressVisibility(false)
        viewState.notifyResourcesChanged()
    }

    private suspend fun onEditedResourceDetected(
        path: Path,
        oldMeta: ResourceMeta
    ) = withContext(Dispatchers.IO) {
        val newMeta = ResourceMeta.fromPath(path).meta ?: return@withContext
        PreviewAndThumbnail.generate(path, newMeta)

        val indexToReplace = resources.indexOf(oldMeta)
        resources[indexToReplace] = newMeta

        index.updateResource(oldMeta.id, path, newMeta)
        tagsStorageRepo.provide(rootAndFav)

        withContext(Dispatchers.Main) {
            viewState.notifyCurrentItemChanged()
            viewState.notifyResourcesChanged()
        }
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

    fun onPlayButtonClick() = presenterScope.launch {
        viewState.viewInExternalApp(index.getPath(currentResource.id))
    }

    fun onBackClick() {
        Log.d(GALLERY_SCREEN, "quitting from GalleryPresenter")
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
