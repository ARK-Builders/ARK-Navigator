package space.taran.arknavigator.mvp.presenter

import android.util.Log
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import ru.terrakok.cicerone.Router
import space.taran.arknavigator.mvp.model.repo.RootAndFav
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndexRepo
import space.taran.arknavigator.mvp.model.repo.preview.PreviewAndThumbnail
import space.taran.arknavigator.mvp.model.repo.tags.PlainTagsStorage
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorage
import space.taran.arknavigator.mvp.presenter.adapter.PreviewsList
import space.taran.arknavigator.mvp.view.GalleryView
import space.taran.arknavigator.mvp.view.item.PreviewItemView
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

    private var isFullscreen = false
    private var workaround = true
    private var currentPos = startAt
    private lateinit var currentResource: ResourceMeta

    private lateinit var index: ResourcesIndex
    private lateinit var storage: TagsStorage
    private lateinit var resources: MutableList<ResourceMeta>

    @Inject
    lateinit var router: Router

    @Inject
    lateinit var indexRepo: ResourcesIndexRepo

    override fun onFirstViewAttach() {
        Log.d(GALLERY_SCREEN, "first view attached in GalleryPresenter")

        // Now the index and storage can only be null if the app dies in the background.
        // In this case, we have to go back and re-index the root on Resources screen.
        index = indexRepo.getFromCache(rootAndFav) ?: let {
            router.exit()
            return
        }
        storage = PlainTagsStorage.getFromCache(rootAndFav) ?: let {
            router.exit()
            return
        }
        resources = resourcesIds.map { index.getMeta(it) }.toMutableList()
        currentResource = resources[startAt]


        val previews = mutableListOf<Path?>()
        val placeholders = mutableListOf<Int>()

        resources.forEach { meta ->
            val path = index.getPath(meta.id)

            previews.add(PreviewAndThumbnail.locate(path, meta)?.preview)
            placeholders.add(ImageUtils.iconForExtension(extension(path)))
        }

        super.onFirstViewAttach()

        viewState.init(PreviewsList(
            previews,
            placeholders,
            resources,
            ::onPreviewsItemClick,
            ::onPreviewsItemZoom,
            ::onPlayButtonClick
        ))

        displayPreview()
    }

    fun onPageChanged(newPos: Int) {
        if (resources.isEmpty())
            return
        if (startAt > 0 || !workaround) {
            //weird bug causes this callback be called redundantly if startAt == 0
            Log.d(GALLERY_SCREEN, "changing to preview at position $newPos")

            if (currentPos != newPos) {
                currentPos = newPos
                currentResource = resources[currentPos]
                displayPreview()
            }
        }
        workaround = false
    }

    fun onTagsChanged(resource: ResourceId) {
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

    fun onRemoveFabClick() {
        Log.d(GALLERY_SCREEN, "[remove_resource] clicked at position $currentPos")
        deleteResource(currentResource.id)

        resources.removeAt(currentPos)
        if (resources.isEmpty()) {
            onBackClick()
            return
        }

        viewState.deleteResource(currentPos)

        if (resources.getOrNull(currentPos) == null)
            currentPos -= 1
        displayPreview()
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
    }

    fun onEditTagsDialogBtnClick() {
        viewState.showEditTagsDialog(currentResource.id)
    }

    fun onSystemUIVisibilityChange(isVisible: Boolean) {
        val newFullscreen = !isVisible
        // prevent loop
        if (isFullscreen == newFullscreen)
            return
        isFullscreen = newFullscreen
        viewState.setFullscreen(isFullscreen)
    }

    private fun deleteResource(resource: ResourceId) = presenterScope.launch(NonCancellable) {
        Log.d(GALLERY_SCREEN, "deleting resource $resource")

        storage.remove(resource)
        val path = index.remove(resource)
        Log.d(GALLERY_SCREEN, "path $path removed from index")

        Files.delete(path)
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
            isFullscreen = true
            viewState.setFullscreen(isFullscreen)
            viewState.setPreviewsScrollingEnabled(false)
        } else
            viewState.setPreviewsScrollingEnabled(true)
    }

    private fun onPreviewsItemClick(itemView: PreviewItemView) {
        Log.d(GALLERY_SCREEN, "preview at ${itemView.pos} clicked, switching controls on/off")
        isFullscreen = !isFullscreen
        viewState.setFullscreen(isFullscreen)
        if (!isFullscreen)
            itemView.resetZoom()
    }

    private fun onPlayButtonClick() {
        viewState.viewInExternalApp(index.getPath(currentResource.id))
    }

    fun onBackClick(): Boolean {
        Log.d(GALLERY_SCREEN, "quitting from GalleryPresenter")
        router.exit()
        viewState.setFullscreen(false)
        return true
    }
}