package space.taran.arkbrowser.mvp.presenter

import android.util.Log
import space.taran.arkbrowser.mvp.presenter.adapter.IDetailListPresenter
import space.taran.arkbrowser.mvp.view.DetailView
import space.taran.arkbrowser.mvp.view.item.DetailItemView
import space.taran.arkbrowser.utils.Constants.Companion.EMPTY_TAG
import space.taran.arkbrowser.utils.Converters.Companion.tagsFromString
import space.taran.arkbrowser.mvp.model.entity.room.ResourceId

import ru.terrakok.cicerone.Router
import moxy.MvpPresenter

import javax.inject.Inject

class DetailPresenter(val resources: List<ResourceId>, val pos: Int) :
    MvpPresenter<DetailView>() {

    @Inject
    lateinit var router: Router

    private var currentResource: ResourceId = resources[0]

    val detailListPresenter = DetailListPresenter()

    inner class DetailListPresenter :
        IDetailListPresenter {

        var resource = mutableListOf<ResourceId>()

        override fun getCount() = resource.size

        override fun bindView(view: DetailItemView) {
            val resource = resource[view.pos]
            Log.d("flow", "[mock] binding view for resource $resource")
            //view.setImage(resource.file)
        }
    }

    override fun onFirstViewAttach() {
        Log.d("flow", "first view attached in DetailPresenter")
        super.onFirstViewAttach()
        viewState.init()

        detailListPresenter.resource = resources.toMutableList()
        viewState.updateAdapter()
        viewState.setCurrentItem(pos)
    }

    fun imageChanged(newPos: Int) {
        currentResource = resources[newPos]
        Log.d("flow", "[mock] image changed to $currentResource")
        //viewState.setImageTags(currentResource.tags)
        //viewState.setTitle(currentResource.name)
    }

    fun fabClicked() {
        Log.d("flow", "[mock] fab clicked on $currentResource")
        //viewState.showTagsDialog(currentResource.tags)
    }

    fun chipGroupClicked() {
        Log.d("flow", "[mock] chip group clicked on $currentResource")
        //viewState.showTagsDialog(currentResource.tags)
    }

    fun tagRemoved(tag: String) {
        Log.d("flow", "[mock] tag $tag removed from $currentResource")
//        currentResource.tags = currentResource.tags - tag
//        roomRepo.insertResource(currentResource)
//
//        viewState.setImageTags(currentResource.tags)
//        viewState.setDialogTags(currentResource.tags)
    }

    // returns true if tags were actually added
    fun tagsAdded(input: String): Boolean {
        val tags = tagsFromString(input)
        if (tags.isEmpty() || tags.contains(EMPTY_TAG)) {
            return false
        }
        Log.d("flow", "[mock] tags $tags added to $currentResource")
//        currentResource.tags = currentResource.tags.plus(tags)
//
//        if (currentResource.synchronized)
//            roomRepo.insertResource(currentResource)
//
//        if (root.synchronized) {
//            resourcesRepo.writeToStorageAsync(root.storage, root.resources)
//            roomRepo.insertRoot(root)
//        }
//
//        viewState.setImageTags(currentResource.tags)
//        viewState.setDialogTags(currentResource.tags)
        viewState.closeDialog()

        return true
    }

    fun dismissDialog() {
        Log.d("flow", "dialog dismissed in DetailPresenter")
        viewState.closeDialog()
    }

    fun backClicked(): Boolean {
        Log.d("flow", "back clicked in DetailPresenter")
        router.exit()
        return true
    }
}