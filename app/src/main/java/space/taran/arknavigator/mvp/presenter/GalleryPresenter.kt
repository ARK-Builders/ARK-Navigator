package space.taran.arknavigator.mvp.presenter

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import space.taran.arknavigator.mvp.model.repo.RootAndFav
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndexRepo
import space.taran.arknavigator.mvp.model.repo.preview.PreviewAndThumbnail
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorage
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorageRepo
import space.taran.arknavigator.mvp.presenter.adapter.PreviewsPagerPresenter
import space.taran.arknavigator.mvp.view.GalleryView
import space.taran.arknavigator.mvp.view.item.PreviewItemView
import space.taran.arknavigator.navigation.AppRouter
import space.taran.arknavigator.utils.GALLERY_SCREEN
import space.taran.arknavigator.utils.ImageUtils
import space.taran.arknavigator.utils.Tag
import space.taran.arknavigator.utils.extension
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject

class GalleryPresenter(
    private val rootAndFav: RootAndFav,
    private val resourcesIds: List<ResourceId>,
    private val startAt: Int
) : MvpPresenter<GalleryView>() {

    private var isControlsVisible = false
    private var currentPos = startAt
    private lateinit var currentResource: ResourceMeta

    lateinit var index: ResourcesIndex
        private set
    lateinit var storage: TagsStorage
        private set
    private lateinit var resources: MutableList<ResourceMeta>

    val previewsPresenter = PreviewsPagerPresenter(viewState)

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
            currentResource = resources[startAt]


            val previews = mutableListOf<Path?>()
            val placeholders = mutableListOf<Int>()

            resources.forEach { meta ->
                val path = index.getPath(meta.id)

                previews.add(PreviewAndThumbnail.locate(path, meta)?.preview)
                placeholders.add(ImageUtils.iconForExtension(extension(path)))
            }

            previewsPresenter.init(
                previews,
                placeholders,
                resources,
                ::onPreviewsItemClick,
                ::onPreviewsItemZoom,
                ::onPlayButtonClick
            )

            viewState.setProgressVisibility(false)

            if (currentPos != 0)
                displayPreview()
        }
    }

    fun onPageChanged(newPos: Int) {
        if (resources.isEmpty())
            return

        currentPos = newPos
        currentResource = resources[currentPos]
        displayPreview()
    }

    fun onTagsChanged() {
        val tags = storage.getTags(currentResource.id)
        viewState.displayPreviewTags(currentResource.id, tags)
    }

    fun onOpenFabClick() {
        Log.d(GALLERY_SCREEN, "[open_resource] clicked at position $currentPos")
        viewState.viewInExternalApp(index.getPath(currentResource.id))
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

        viewState.deleteResource(currentPos)
    }

    fun onShareFabClick() {
        Log.d(GALLERY_SCREEN, "[share_resource] clicked at position $currentPos")
        viewState.shareResource(index.getPath(currentResource.id))
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

    private suspend fun deleteResource(resource: ResourceId)  {
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

    private fun onPreviewsItemZoom(zoomed: Boolean) {
        if (zoomed) {
            isControlsVisible = false
            viewState.setControlsVisibility(isControlsVisible)
            viewState.setPreviewsScrollingEnabled(false)
        } else
            viewState.setPreviewsScrollingEnabled(true)
    }

    private fun onPreviewsItemClick(itemView: PreviewItemView) {
        Log.d(GALLERY_SCREEN, "preview at ${itemView.pos} clicked, switching controls on/off")
        isControlsVisible = !isControlsVisible
        viewState.setControlsVisibility(isControlsVisible)
        if (!isControlsVisible)
            itemView.resetZoom()
    }

    private fun onPlayButtonClick() {
        viewState.viewInExternalApp(index.getPath(currentResource.id))
    }

    fun onBackClick() {
        Log.d(GALLERY_SCREEN, "quitting from GalleryPresenter")
        viewState.exitFullscreen()
        router.exit()
    }
}