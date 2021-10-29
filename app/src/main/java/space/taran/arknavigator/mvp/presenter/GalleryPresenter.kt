package space.taran.arknavigator.mvp.presenter

import android.util.Log
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import ru.terrakok.cicerone.Router
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.repo.*
import space.taran.arknavigator.ui.fragments.utils.Preview
import space.taran.arknavigator.mvp.presenter.adapter.PreviewsList
import space.taran.arknavigator.mvp.view.GalleryView
import space.taran.arknavigator.mvp.view.item.PreviewItemView
import space.taran.arknavigator.utils.GALLERY_SCREEN
import space.taran.arknavigator.utils.Tags
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject

class GalleryPresenter(
    private val index: ResourcesIndex,
    private val storage: TagsStorage,
    private val resources: MutableList<ResourceId>
) : MvpPresenter<GalleryView>() {

    private var isFullscreen = false

    @Inject
    lateinit var router: Router

    @Inject
    lateinit var previewsRepo: PreviewsRepo

    private lateinit var previews: PreviewsList

    override fun onFirstViewAttach() {
        val previewList = arrayListOf<Preview>()
        val resourceMetas = arrayListOf<ResourceMeta?>()

        resources.forEach { id ->
            val path = index.getPath(id)
            val meta = index.getMeta(id)

            previewList.add(previewsRepo.providePreview(path!!, meta))
            resourceMetas.add(meta)
        }

        previews = PreviewsList(
            previewList,
            resourceMetas,
            ::onPreviewsItemClick,
            ::onPreviewsItemZoom,
            ::onPlayButtonClick
        )

        Log.d(GALLERY_SCREEN, "first view attached in GalleryPresenter")
        super.onFirstViewAttach()
        viewState.init(previews)
    }

    fun deleteResource(resource: ResourceId) = presenterScope.launch(NonCancellable) {
        Log.d(GALLERY_SCREEN, "deleting resource $resource")

        storage.remove(resource)
        val path = index.remove(resource)
        Log.d(GALLERY_SCREEN, "path $path removed from index")

        Files.delete(path)
    }

    fun listTags(resource: ResourceId): Tags {
        val tags = storage.getTags(resource)
        Log.d(GALLERY_SCREEN, "resource $resource has tags $tags")
        return tags
    }

    fun replaceTags(resource: ResourceId, tags: Tags) = presenterScope.launch(NonCancellable) {
        Log.d(GALLERY_SCREEN, "tags $tags set to $resource")
        storage.setTags(resource, tags)
    }

    fun onSystemUIVisibilityChange(isVisible: Boolean) {
        val newFullscreen = !isVisible
        // prevent loop
        if (isFullscreen == newFullscreen)
            return
        isFullscreen = newFullscreen
        viewState.setFullscreen(isFullscreen)
    }

    fun getExtraInfoAt(position: Int, path: Path?): MutableMap<Preview.ExtraInfoTag, String>? =
        previews.getExtraMetaAt(position)?.extra?.appData(path)

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

    private fun onPlayButtonClick(position: Int) {
        viewState.openResourceDetached(position)
    }

    fun quit(): Boolean {
        Log.d(GALLERY_SCREEN, "quitting from GalleryPresenter")
        router.exit()
        viewState.setFullscreen(false)
        return true
    }
}