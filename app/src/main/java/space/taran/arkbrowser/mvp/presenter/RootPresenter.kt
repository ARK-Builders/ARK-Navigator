package space.taran.arkbrowser.mvp.presenter

import space.taran.arkbrowser.mvp.model.entity.*
import space.taran.arkbrowser.mvp.model.entity.common.Icon
import space.taran.arkbrowser.mvp.model.entity.room.SDCardUri
import space.taran.arkbrowser.mvp.model.repo.ResourcesRepo
import space.taran.arkbrowser.mvp.model.repo.RoomRepo
import space.taran.arkbrowser.mvp.model.repo.RootsRepo
import space.taran.arkbrowser.mvp.model.entity.common.IconOrImage
import space.taran.arkbrowser.mvp.presenter.adapter.IItemGridPresenter
import space.taran.arkbrowser.mvp.view.RootView
import space.taran.arkbrowser.mvp.view.item.FileItemView
import space.taran.arkbrowser.navigation.Screens
import space.taran.arkbrowser.utils.resourceComparator
import space.taran.arkbrowser.utils.SortBy
import space.taran.arkbrowser.utils.listChildren
import moxy.InjectViewState
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import java.io.File
import java.lang.IllegalStateException
import javax.inject.Inject

@InjectViewState
class RootPresenter: MvpPresenter<RootView>() {
    @Inject
    lateinit var router: Router

    @Inject
    lateinit var roomRepo: RoomRepo

    @Inject
    lateinit var rootsRepo: RootsRepo

    @Inject
    lateinit var resourcesRepo: ResourcesRepo

    val rootGridPresenter = ItemGridPresenter()
    val dialogGridPresenter = DialogItemGridPresenter()
    var pickedDir: File? = null
    var dialogIsOpen: Boolean = false

    inner class ItemGridPresenter :
        IItemGridPresenter {

        private var roots: List<Root> = listOf()

        fun load(roots: List<Root>) {
            this.roots = roots
        }

        override fun getCount() = roots.size

        override fun bindView(view: FileItemView) {
            val root = roots[view.pos]
            view.setText(root.name)
            view.setIcon(IconOrImage(icon = Icon.ROOT))
        }

        override fun itemClicked(pos: Int) {
            val root = roots[pos]
            router.replaceScreen(Screens.TagsScreen(
                rootName = root.name,
                resources = resourcesRepo.retrieveResources(root.folder)))
        }
    }

    inner class DialogItemGridPresenter: IItemGridPresenter {
        var files = mutableListOf<Resource>()

        override fun getCount() = files.size

        override fun bindView(view: FileItemView) {
            val file = files[view.pos]
            view.setText(file.name)
            if (file.isFolder)
                view.setIcon(Icon.FOLDER, null)
            else {
                if (file.isImage())
                    view.setIcon(Icon.IMAGE, file.file)
                else
                    view.setIcon(Icon.FILE, file.file)
            }
        }

        override fun itemClicked(pos: Int) {
            val file = files[pos]
            if (file.isFolder) {
                pickedDir = file.file
                viewState.setDialogPath(pickedDir!!)
                files.clear()
                files.addAll(resourcesRepo.fileDataSource.list(file.file)
                    .sortedWith(resourceComparator(SortBy.NAME)))
                viewState.updateDialogAdapter()
            }
        }
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.init()
    }

    fun onViewResumed() {
        rootGridPresenter.load(rootsRepo.getRoots().sortedBy { it.name })
        viewState.updateRootAdapter()
    }

    fun rootPicked() {
        if (pickedDir == null) {
            throw IllegalStateException("Nothing is really picked")
        }

        val root = Root(name = pickedDir!!.name, folder = pickedDir!!)
        rootsRepo.insertRoot(root)

        val storage = resourcesRepo.createStorage(pickedDir!!)
        if (storage == null) {
            requestSdCardUri()
            return
        }

        rootsRepo.synchronizeRoot(root)
        rootGridPresenter.roots.clear()
        val sortedRoots = rootsRepo.roots.toMutableList()
        sortedRoots.sortBy { it.name }
        rootGridPresenter.roots.addAll(sortedRoots)
        viewState.updateRootAdapter()
        dismissDialog()
    }

    fun dismissDialog() {
        viewState.closeChooserDialog()
        dialogIsOpen = false
    }

    fun fabClicked() {
        viewState.openChooserDialog()
        dialogIsOpen = true
        pickedDir = null
        dialogGridPresenter.files.clear()
        dialogGridPresenter.files.addAll(resourcesRepo.fileDataSource.getExtSdCards())
        viewState.updateDialogAdapter()
    }

    private fun requestSdCardUri() {
        val basePath = resourcesRepo.fileDataSource.getExtSdCardBaseFolder(pickedDir!!.path)
        roomRepo.getSdCardUriByPath(basePath!!).observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                it.uri = null
                roomRepo.insertSdCardUri(it).observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ viewState.requestSdCardUri() }, {})
            }, {
                roomRepo.insertSdCardUri(SDCardUri(path = basePath))
                    .observeOn(AndroidSchedulers.mainThread()).subscribe(
                        { viewState.requestSdCardUri() }, {})
            })
    }

    fun backClicked(): Boolean {
        if (dialogIsOpen) {
            if (pickedDir != null) {
                val extPaths = resourcesRepo.fileDataSource.getExtSdCards()
                extPaths.forEach {
                    if (pickedDir == it.path) {
                        pickedDir = null
                        dialogGridPresenter.files.clear()
                        dialogGridPresenter.files.addAll(extPaths)
                        viewState.updateDialogAdapter()
                        viewState.setDialogPath("/")
                        return true
                    }
                }

                pickedDir = pickedDir.parentFile
                val files = listChildren(pickedDir!!)
                viewState.setDialogPath(pickedDir!!)
                dialogGridPresenter.files.clear()
                dialogGridPresenter.files.addAll(
                    files.sortedWith(resourceComparator(SortBy.NAME)))
                viewState.updateDialogAdapter()
            } else {
                viewState.closeChooserDialog()
            }
        } else {
            router.exit()
        }

        return true
    }

}