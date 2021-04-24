package space.taran.arkbrowser.mvp.presenter

import space.taran.arkbrowser.mvp.model.entity.Resource
import space.taran.arkbrowser.mvp.model.entity.Root
import space.taran.arkbrowser.mvp.model.repo.ResourcesRepo
import space.taran.arkbrowser.mvp.model.repo.RoomRepo
import space.taran.arkbrowser.mvp.presenter.adapter.IDetailListPresenter
import space.taran.arkbrowser.mvp.view.DetailView
import space.taran.arkbrowser.mvp.view.item.DetailItemView
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import space.taran.arkbrowser.utils.Constants.Companion.EMPTY_TAG
import space.taran.arkbrowser.utils.Converters.Companion.tagsFromString
import javax.inject.Inject

class DetailPresenter(val root: Root, val resources: List<Resource>, val pos: Int) :
    MvpPresenter<DetailView>() {

    @Inject
    lateinit var resourcesRepo: ResourcesRepo

    @Inject
    lateinit var router: Router

    @Inject
    lateinit var roomRepo: RoomRepo

    private var currentResource: Resource = resources[0]

    val detailListPresenter = DetailListPresenter()

    inner class DetailListPresenter :
        IDetailListPresenter {

        var files = mutableListOf<Resource>()

        override fun getCount() = files.size

        override fun bindView(view: DetailItemView) {
            val image = files[view.pos]
            view.setImage(image.file)
        }
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.init()

        detailListPresenter.files = resources.toMutableList()
        viewState.updateAdapter()
        viewState.setCurrentItem(pos)
    }

    fun imageChanged(newPos: Int) {
        currentResource = resources[newPos]
        viewState.setImageTags(currentResource.tags)
        viewState.setTitle(currentResource.name)
    }

    fun fabClicked() {
        viewState.showTagsDialog(currentResource.tags)
    }

    fun chipGroupClicked() {
        viewState.showTagsDialog(currentResource.tags)
    }

    fun tagRemoved(tag: String) {
        currentResource.tags = currentResource.tags - tag
        roomRepo.insertResource(currentResource)

        viewState.setImageTags(currentResource.tags)
        viewState.setDialogTags(currentResource.tags)
    }

    // returns true if tags were actually added
    fun tagsAdded(input: String): Boolean {
        val tags = tagsFromString(input)
        if (tags.isEmpty() || tags.contains(EMPTY_TAG)) {
            return false
        }

        currentResource.tags = currentResource.tags.plus(tags)

        if (currentResource.synchronized)
            roomRepo.insertResource(currentResource)

        if (root.synchronized) {
            resourcesRepo.writeToStorageAsync(root.storage, root.resources)
            roomRepo.insertRoot(root)
        }

        viewState.setImageTags(currentResource.tags)
        viewState.setDialogTags(currentResource.tags)
        viewState.closeDialog()

        return true
    }

    fun dismissDialog() {
        viewState.closeDialog()
    }

    fun backClicked(): Boolean {
        router.exit()
        return true
    }
}