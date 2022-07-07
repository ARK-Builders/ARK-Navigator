package space.taran.arknavigator.mvp.presenter

import android.util.Log
import androidx.recyclerview.widget.DiffUtil
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.notExists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
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
import space.taran.arknavigator.mvp.model.repo.preview.PreviewAndThumbnail

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
            storage = tagsStorageRepo.provide(rootAndFav)
            resources = resourcesIds.map { index.getMeta(it) }.toMutableList()

            viewState.updatePagerAdapter()
            viewState.setProgressVisibility(false)
        }
    }

    fun onPageChanged(newPos: Int) {
        if (resources.isEmpty())
            return

        val path = index.getPath(resources[newPos].id)
        if (path.notExists())
            onRemovedResourceDetected()

        currentPos = newPos
        displayPreview()
    }

    fun detectItemType(pos: Int) = when (resources[pos].kind) {
        is ResourceKind.PlainText -> GalleryItemType.PLAINTEXT
        else -> GalleryItemType.OTHER
    }

    fun bindView(view: PreviewItemView) {
        view.reset()
        val meta = resources[view.pos]
        val path = index.getPath(meta.id)
        if (meta.modified != path.getLastModifiedTime()) {
            presenterScope.launch {
                val newResourceMeta = reindexResource(pos = view.pos, oldMeta = meta)
                view.setSource(path, newResourceMeta)
            }
        } else {
            view.setSource(path, meta)
        }
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

    fun onOpenFabClick() {
        Log.d(GALLERY_SCREEN, "[open_resource] clicked at position $currentPos")
        val kind = currentResource.kind
        if (kind is ResourceKind.Link) {
            val url = kind.url ?: return
            viewState.openLink(url)
            return
        }

        viewState.viewInExternalApp(index.getPath(currentResource.id))
    }

    fun onShareFabClick() {
        Log.d(GALLERY_SCREEN, "[share_resource] clicked at position $currentPos")
        val kind = currentResource.kind
        if (kind is ResourceKind.Link) {
            val url = kind.url ?: return
            viewState.shareLink(url)
            return
        }

        viewState.shareResource(index.getPath(currentResource.id))
    }

    fun onEditFabClick() {
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
        storage.setTags(currentResource.id, newTags)
        viewState.notifyTagsChanged()
    }

    fun onEditTagsDialogBtnClick() {
        viewState.showEditTagsDialog(currentResource.id)
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

    private fun displayPreview() {
        val resource = resources[currentPos]
        val tags = storage.getTags(resource.id)
        val filePath = index.getPath(resource.id)
        viewState.setupPreview(currentPos, resource, filePath.fileName.toString())
        viewState.displayPreviewTags(resource.id, tags)
    }

    private fun onRemovedResourceDetected() = presenterScope.launch {
        viewState.setProgressVisibility(true, "Indexing")

        index.reindex()
        // update current tags storage
        tagsStorageRepo.provide(rootAndFav)
        invalidateResources()
        viewState.setProgressVisibility(false)
        viewState.notifyResourcesChanged()
    }

    private fun invalidateResources() {
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

    fun onPlayButtonClick() {
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

    private suspend fun reindexResource(pos: Int, oldMeta: ResourceMeta):
        ResourceMeta {
        val path = index.getPath(oldMeta.id)
        ResourceMeta.fromPath(path)?.let { newMeta ->
            if (oldMeta.id != newMeta.id) {
                val tags = storage.getTags(oldMeta.id)
                index.updateResource(oldMeta.id, path, newMeta)
                // update current tags storage
                tagsStorageRepo.provide(rootAndFav)
                storage.remove(oldMeta.id)
                storage.setTags(newMeta.id, tags)
                PreviewAndThumbnail.forget(oldMeta.id)
                withContext(presenterScope.coroutineContext + Dispatchers.IO) {
                    PreviewAndThumbnail.generate(path, newMeta)
                }
                resources[pos] = newMeta
                // refresh cached data using DB
                indexRepo.loadFromDatabase(rootAndFav.root!!)
                index = indexRepo.provide(rootAndFav)
                storage = tagsStorageRepo.provide(rootAndFav)
                val indexedIds = index.listIds(rootAndFav.fav)
                val newResources = indexedIds.map {
                    index.getMeta(it)
                }.toMutableList()
                currentPos = newResources.indexOf(
                    newResources.find { it.id == newMeta.id }
                )
                diffResult = DiffUtil.calculateDiff(
                    ResourceMetaDiffUtilCallback(
                        resources,
                        newResources
                    )
                )
                resources = newResources
                // finish
                viewState.notifyResourcesOrderChanged()
                viewState.updatePagerAdapterWithDiff()
                return newMeta
            }
        }
        return oldMeta
    }
}
