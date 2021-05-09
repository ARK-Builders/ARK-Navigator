package space.taran.arkbrowser.mvp.presenter

import android.util.Log
import space.taran.arkbrowser.mvp.model.entity.common.Icon
import space.taran.arkbrowser.mvp.model.entity.common.IconOrImage
import space.taran.arkbrowser.mvp.view.RootView
import space.taran.arkbrowser.mvp.view.item.FileItemView
import moxy.InjectViewState
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import space.taran.arkbrowser.mvp.model.repo.FoldersRepo
import space.taran.arkbrowser.mvp.presenter.adapter.ItemGridPresenter
import space.taran.arkbrowser.utils.ROOTS_SCREEN
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

    val rootGridPresenter = XItemGridPresenter()

    private val roots = mutableListOf<Path>()

    //todo
    var pickedDir: File? = null

    inner class XItemGridPresenter :
        ItemGridPresenter<Unit, Path>({
            Log.d(ROOTS_SCREEN, "[mock] creating Tags screen with $it")
//            router.replaceScreen(Screens.TagsScreen(
//                resources = resourcesRepo.retrieveResources(root.folder)))

        }) {

        private var roots: List<Path> = listOf()

        override fun items() = roots

        override fun updateItems(label: Unit, items: List<Path>) {
            roots = items
        }

        override fun bindView(view: FileItemView) {
            Log.d(ROOTS_SCREEN, "binding view in RootsPresenter/ItemGridPresenter")
            val root = roots[view.pos]
            //view.setText(root.name)
            view.setIcon(IconOrImage(icon = Icon.ROOT))
        }

        override fun backClicked(): Unit {
            TODO("Not yet implemented")
        }
    }

    override fun onFirstViewAttach() {
        Log.d(ROOTS_SCREEN, "first view attached in RootsPresenter")
        super.onFirstViewAttach()
        viewState.init()
    }

    fun onViewResumed() {
        Log.d(ROOTS_SCREEN, "[mock] view resumed in RootsPresenter")
//        rootGridPresenter.load(rootsRepo.getRoots().sortedBy { it.name })
        viewState.updateAdapter()
    }

    fun rootPicked() {
        if (pickedDir == null) {
            throw IllegalStateException("Nothing is really picked")
        }

        Log.d(ROOTS_SCREEN, "[mock] root $pickedDir picked in RootsPresenter")

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
    }

    fun fabClicked() {
        Log.d(ROOTS_SCREEN, "[add root] clicked")
        viewState.openRootPicker(devices) {
            Log.d(ROOTS_SCREEN, "path $it was added as root")
            roots.add(it)

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

        }

        pickedDir = null
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
        Log.d(ROOTS_SCREEN, "back clicked")
        router.exit()
        return true
    }
}