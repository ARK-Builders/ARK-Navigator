package space.taran.arkbrowser.mvp.presenter

import android.util.Log
import space.taran.arkbrowser.mvp.model.entity.common.Icon
import space.taran.arkbrowser.mvp.model.entity.common.IconOrImage
import space.taran.arkbrowser.mvp.presenter.adapter.IItemGridPresenter
import space.taran.arkbrowser.mvp.view.RootView
import space.taran.arkbrowser.mvp.view.item.FileItemView
import moxy.InjectViewState
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import space.taran.arkbrowser.mvp.model.repo.FoldersRepo
import java.io.File
import java.lang.IllegalStateException
import java.nio.file.Path
import javax.inject.Inject

@InjectViewState
class RootsPresenter(private val devices: List<Path>): MvpPresenter<RootView>() {
    @Inject
    lateinit var router: Router

    @Inject
    lateinit var foldersRepo: FoldersRepo

    val rootGridPresenter = ItemGridPresenter()

    private val roots = mutableListOf<Path>()

    //todo
    var pickedDir: File? = null
    var dialogIsOpen: Boolean = false

    inner class ItemGridPresenter :
        IItemGridPresenter<Path>({
            Log.d("flow", "[mock] creating Tags screen with $it")
//            router.replaceScreen(Screens.TagsScreen(
//                resources = resourcesRepo.retrieveResources(root.folder)))

        }) {

        private var roots: List<Path> = listOf()

        override fun items() = roots

        override fun bindView(view: FileItemView) {
            Log.d("flow", "binding view in RootsPresenter/ItemGridPresenter")
            val root = roots[view.pos]
            //view.setText(root.name)
            view.setIcon(IconOrImage(icon = Icon.ROOT))
        }

        override fun backClicked() {
            TODO("Not yet implemented")
        }
    }

    override fun onFirstViewAttach() {
        Log.d("flow", "first view attached in RootsPresenter")
        super.onFirstViewAttach()
        viewState.init()
    }

    fun onViewResumed() {
        Log.d("flow", "[mock] view resumed in RootsPresenter")
//        rootGridPresenter.load(rootsRepo.getRoots().sortedBy { it.name })
        viewState.updateRootAdapter()
    }

    fun rootPicked() {
        if (pickedDir == null) {
            throw IllegalStateException("Nothing is really picked")
        }

        Log.d("flow", "[mock] root $pickedDir picked in RootsPresenter")

//        val root = remove_Root(name = pickedDir!!.name, folder = pickedDir!!)
//        rootsRepo.insertRoot(root)
//
//        val storage = resourcesRepo.createStorage(pickedDir!!)
//        if (storage == null) {
//            requestSdCardUri()
//            return
//        }
//
//        rootsRepo.synchronizeRoot(root)
//        rootGridPresenter.roots.clear()
//        val sortedRoots = rootsRepo.roots.toMutableList()
//        sortedRoots.sortBy { it.name }
//        rootGridPresenter.roots.addAll(sortedRoots)
//        viewState.updateRootAdapter()
        dismissDialog()
    }

    fun dismissDialog() {
        Log.d("flow", "dialog dismissed in RootsPresenter")
        viewState.closeChooserDialog()
        dialogIsOpen = false
    }

    fun fabClicked() {
        Log.d("flow", "fab clicked in RootsPresenter")
        viewState.openChooserDialog(devices) {
            Log.d("flow", "Path $it was added as root")
            roots.add(it)
        }
        dialogIsOpen = true
        pickedDir = null
        viewState.updateDialogAdapter()
    }

    private fun requestSdCardUri() {
        //todo
//        val basePath = resourcesRepo.fileDataSource.getExtSdCardBaseFolder(pickedDir!!.path)
//        roomRepo.getSdCardUriByPath(basePath!!).observeOn(AndroidSchedulers.mainThread())
//            .subscribe({
//                it.uri = null
//                roomRepo.insertSdCardUri(it).observeOn(AndroidSchedulers.mainThread())
//                    .subscribe({ viewState.requestSdCardUri() }, {})
//            }, {
//                roomRepo.insertSdCardUri(SDCardUri(path = basePath))
//                    .observeOn(AndroidSchedulers.mainThread()).subscribe(
//                        { viewState.requestSdCardUri() }, {})
//            })
    }

    fun backClicked(): Boolean {
        Log.d("flow", "back clicked in RootsPresenter")
        if (dialogIsOpen) {
            Log.d("flow", "dialog is open in RootsPresenter")
//            if (pickedDir != null) {
//                val extPaths = resourcesRepo.fileDataSource.getExtSdCards()
//                extPaths.forEach {
//                    if (pickedDir == it.path) {
//                        pickedDir = null
//                        dialogGridPresenter.files.clear()
//                        dialogGridPresenter.files.addAll(extPaths)
//                        viewState.updateDialogAdapter()
//                        viewState.setDialogPath("/")
//                        return true
//                    }
//                }
//
//                pickedDir = pickedDir.parentFile
//                val files = listChildren(pickedDir!!)
//                viewState.setDialogPath(pickedDir!!)
//                dialogGridPresenter.files.clear()
//                dialogGridPresenter.files.addAll(
//                    files.sortedWith(resourceComparator(SortBy.NAME)))
//                viewState.updateDialogAdapter()
//            } else {
//                viewState.closeChooserDialog()
//            }
        } else {
            Log.d("flow", "dialog isn't open in RootsPresenter, quitting")
            router.exit()
        }

        return true
    }

}