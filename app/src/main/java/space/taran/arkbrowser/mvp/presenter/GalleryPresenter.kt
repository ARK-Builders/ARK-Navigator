package space.taran.arkbrowser.mvp.presenter

import android.util.Log
import space.taran.arkbrowser.mvp.view.GalleryView
import space.taran.arkbrowser.mvp.model.dao.ResourceId

import ru.terrakok.cicerone.Router
import moxy.MvpPresenter
import space.taran.arkbrowser.mvp.model.dao.common.Preview
import space.taran.arkbrowser.mvp.model.repo.ResourcesIndex
import space.taran.arkbrowser.mvp.model.repo.TagsStorage
import space.taran.arkbrowser.mvp.presenter.adapter.PreviewsList
import space.taran.arkbrowser.mvp.presenter.adapter.ResourcesList
import space.taran.arkbrowser.utils.GALLERY_SCREEN
import space.taran.arkbrowser.utils.Tags
import java.nio.file.Files

import javax.inject.Inject

typealias PreviewClickHandler = () -> Unit

class GalleryPresenter(
    private val index: ResourcesIndex,
    private val storage: TagsStorage,
    resources: ResourcesList,
    handler: PreviewClickHandler)
    : MvpPresenter<GalleryView>() {

    @Inject
    lateinit var router: Router

    private val previews = PreviewsList(resources.items().map {
        Preview.provide(index.getPath(it)!!)
    }) { _, _ -> handler() }

    override fun onFirstViewAttach() {
        Log.d(GALLERY_SCREEN, "first view attached in GalleryPresenter")
        super.onFirstViewAttach()
        viewState.init(previews)
    }

    fun deleteResource(resource: ResourceId) {
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

    fun replaceTags(resource: ResourceId, tags: Tags) {
        Log.d(GALLERY_SCREEN, "tags $tags added to $resource")
        storage.setTags(resource, tags)
    }

    fun quit(): Boolean {
        Log.d(GALLERY_SCREEN, "quitting from GalleryPresenter")
        router.exit()
        return true
    }
}