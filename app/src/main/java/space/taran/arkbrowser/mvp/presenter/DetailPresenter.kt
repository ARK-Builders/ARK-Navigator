package space.taran.arkbrowser.mvp.presenter

import space.taran.arkbrowser.mvp.model.entity.File
import space.taran.arkbrowser.mvp.model.entity.Root
import space.taran.arkbrowser.mvp.model.repo.FilesRepo
import space.taran.arkbrowser.mvp.model.repo.RoomRepo
import space.taran.arkbrowser.mvp.presenter.adapter.IDetailListPresenter
import space.taran.arkbrowser.mvp.view.DetailView
import space.taran.arkbrowser.mvp.view.item.DetailItemView
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import space.taran.arkbrowser.utils.Constants.Companion.EMPTY_TAG
import space.taran.arkbrowser.utils.Converters.Companion.tagsFromString
import javax.inject.Inject

class DetailPresenter(val root: Root, val files: List<File>, val pos: Int) :
    MvpPresenter<DetailView>() {

    @Inject
    lateinit var filesRepo: FilesRepo

    @Inject
    lateinit var router: Router

    @Inject
    lateinit var roomRepo: RoomRepo

    var currentFile: File? = null

    val detailListPresenter = DetailListPresenter()

    inner class DetailListPresenter :
        IDetailListPresenter {

        var files = mutableListOf<File>()

        override fun getCount() = files.size

        override fun bindView(view: DetailItemView) {
            val image = files[view.pos]
            view.setImage(image.path)
        }
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.init()

        detailListPresenter.files = files.toMutableList()
        viewState.updateAdapter()
        viewState.setCurrentItem(pos)
    }

    fun imageChanged(newPos: Int) {
        currentFile = files[newPos]
        viewState.setImageTags(currentFile!!.tags)
        viewState.setTitle(currentFile!!.name)
    }

    fun fabClicked() {
        viewState.showTagsDialog(currentFile!!.tags)
    }

    fun chipGroupClicked() {
        viewState.showTagsDialog(currentFile!!.tags)
    }

    fun tagRemoved(tag: String) {
        currentFile!!.tags = currentFile!!.tags.minus(tag)
        if (currentFile!!.synchronized)
            roomRepo.insertFile(currentFile!!).subscribe()

        if (root.synchronized) {
            filesRepo.writeToStorageAsync(root.storagePath, root.files).subscribe(
                {
                    root.storageLastModified = filesRepo.getRootLastModified(root)
                    roomRepo.insertRoot(root).subscribe()
                },
                {}
            )
        }

        viewState.setImageTags(currentFile!!.tags)
        viewState.setDialogTags(currentFile!!.tags)
    }

    // returns true if tags were actually added
    fun tagsAdded(input: String): Boolean {
        val tags = tagsFromString(input)
        if (tags.isEmpty() || tags.contains(EMPTY_TAG)) {
            return false
        }

        currentFile!!.tags = currentFile!!.tags.plus(tags)

        if (currentFile!!.synchronized)
            roomRepo.insertFile(currentFile!!).subscribe()

        if (root.synchronized) {
            filesRepo.writeToStorageAsync(root.storagePath, root.files).subscribe(
                {
                    root.storageLastModified = filesRepo.getRootLastModified(root)
                    roomRepo.insertRoot(root).subscribe()
                },
                {}
            )
        }

        viewState.setImageTags(currentFile!!.tags)
        viewState.setDialogTags(currentFile!!.tags)
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