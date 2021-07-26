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
import space.taran.arkbrowser.utils.GALLERY_SCREEN
import space.taran.arkbrowser.utils.Tags

import javax.inject.Inject

class GalleryPresenter(
    private val index: ResourcesIndex,
    private val storage: TagsStorage,
    private val resources: List<ResourceId>)
    : MvpPresenter<GalleryView>() {

    @Inject
    lateinit var router: Router

    private val previews = PreviewsList(resources.map {
        Preview.provide(index.getPath(it)!!)
    })

    override fun onFirstViewAttach() {
        Log.d(GALLERY_SCREEN, "first view attached in GalleryPresenter")
        super.onFirstViewAttach()
        viewState.init(previews)
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

    fun backClicked(): Boolean {
        Log.d(GALLERY_SCREEN, "[back] clicked in GalleryPresenter")
        router.exit()
        return true
    }
}