package space.taran.arknavigator.mvp.presenter

import android.util.Log
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import ru.terrakok.cicerone.Router
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.dao.common.Preview
import space.taran.arknavigator.mvp.model.repo.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.TagsStorage
import space.taran.arknavigator.mvp.presenter.adapter.PreviewsList
import space.taran.arknavigator.mvp.presenter.adapter.ResourcesList
import space.taran.arknavigator.mvp.view.GalleryView
import space.taran.arknavigator.utils.GALLERY_SCREEN
import space.taran.arknavigator.utils.Tags
import java.nio.file.Files
import javax.inject.Inject

typealias PreviewClickHandler = () -> Unit

class GalleryPresenter(
    private val index: ResourcesIndex,
    private val storage: TagsStorage,
    resources: ResourcesList)
    : MvpPresenter<GalleryView>() {

    private var isFullscreen = false

    @Inject
    lateinit var router: Router

    private val previews = PreviewsList(resources.items().map {
        Preview.provide(index.getPath(it)!!)
    }, ::onPreviewsItemClick)

    override fun onFirstViewAttach() {
        Log.d(GALLERY_SCREEN, "first view attached in GalleryPresenter")
        super.onFirstViewAttach()
        viewState.init(previews)
    }

    fun deleteResource(resource: ResourceId) = presenterScope.launch {
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

    fun replaceTags(resource: ResourceId, tags: Tags) = presenterScope.launch {
        Log.d(GALLERY_SCREEN, "tags $tags added to $resource")
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

     private fun onPreviewsItemClick(pos: Int, preview: Preview) {
        Log.d(GALLERY_SCREEN, "preview clicked, switching controls on/off")
        isFullscreen = !isFullscreen
        viewState.setFullscreen(isFullscreen)
    }

    fun quit(): Boolean {
        Log.d(GALLERY_SCREEN, "quitting from GalleryPresenter")
        router.exit()
        viewState.setFullscreen(false)
        return true
    }
}