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
    private val resources: List<ResourceId>,
    private val initialPosition: Int)
    : MvpPresenter<GalleryView>() {

    @Inject
    lateinit var router: Router

    private var currentResource: ResourceId = resources[0]

    private val previews = PreviewsList(resources.map {
        Preview.provide(index.getPath(it)!!)
    }, initialPosition)

    override fun onFirstViewAttach() {
        Log.d(GALLERY_SCREEN, "first view attached in GalleryPresenter")
        super.onFirstViewAttach()
        viewState.init(previews)
    }

    fun removeTag(tag: String) {
        Log.d(GALLERY_SCREEN, "[mock] tag $tag removed from $currentResource")
        //roomRepo.insertResource(currentResource)
    }

    // returns true if tags were actually added
    fun replaceTags(tags: Tags) {
        Log.d(GALLERY_SCREEN, "[mock] tags $tags added to $currentResource")
        //        currentResource.tags = currentResource.tags.plus(tags)
        //
        //        if (currentResource.synchronized)
        //            roomRepo.insertResource(currentResource)
        //
        //        if (root.synchronized) {
        //            resourcesRepo.writeToStorageAsync(root.storage, root.resources)
        //            roomRepo.insertRoot(root)
        //        }
    }

    fun backClicked(): Boolean {
        Log.d(GALLERY_SCREEN, "[back] clicked in GalleryPresenter")
        router.exit()
        return true
    }
}