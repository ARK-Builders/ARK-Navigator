package space.taran.arkbrowser.mvp.presenter

import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import space.taran.arkbrowser.mvp.model.entity.common.Icon
import space.taran.arkbrowser.mvp.model.entity.common.IconOrImage
import space.taran.arkbrowser.mvp.view.RootView
import space.taran.arkbrowser.mvp.view.item.FileItemView
import moxy.InjectViewState
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import space.taran.arkbrowser.mvp.model.repo.Folders
import space.taran.arkbrowser.mvp.model.repo.FoldersRepo
import space.taran.arkbrowser.mvp.presenter.adapter.ItemGridPresenter
import space.taran.arkbrowser.utils.CONCURRENT
import space.taran.arkbrowser.utils.PartialResult
import space.taran.arkbrowser.utils.ROOTS_SCREEN
import java.nio.file.Path
import javax.inject.Inject

@InjectViewState
class RootsPresenter: MvpPresenter<RootView>() {
    @Inject
    lateinit var router: Router

    @Inject
    lateinit var foldersRepo: FoldersRepo

    private lateinit var folders: PartialResult<Folders, List<Path>>

//    private val rootGridPresenter = XItemGridPresenter()

    override fun onFirstViewAttach() {
        Log.d(ROOTS_SCREEN, "first view attached in RootsPresenter")
        super.onFirstViewAttach()

        Log.d(CONCURRENT, "runBlocking[-1]")
        runBlocking {
            Log.d(CONCURRENT, "runBlocking[0]")
            //            GlobalScope.launch {
            launch {
                Log.d(CONCURRENT, "launch[0]")
                folders = foldersRepo.query()
                //todo protect `folders`?
                Log.d(CONCURRENT, "launch[1]")
            }
            Log.d(CONCURRENT, "runBlocking[1]")
        }
        Log.d(CONCURRENT, "runBlocking[2]")

        Log.d(ROOTS_SCREEN, "folders loaded: $folders")
        viewState.loadFolders(folders.succeeded)

//        viewState.init() //todo load


        //        roomRepo.getAllRoots().observeOn(AndroidSchedulers.mainThread()).subscribe(
//            { list ->
//                list.forEach { root ->
//                    val storageVersion = resourcesRepo.readStorageVersion(root.storage)
//                    if (storageVersion != ResourcesRepo.STORAGE_VERSION)
//                        storageVersionDifferent(storageVersion, root)
//                    rootsRepo.synchronizeRoot(root)
//                }
//            },
//            {}
//        )

    }

    fun addRoot(path: Path) {
        Log.d(ROOTS_SCREEN, "[mock] root $path picked in RootsPresenter")

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

    fun resume() {
        Log.d(ROOTS_SCREEN, "[mock] view resumed in RootsPresenter")
//        rootGridPresenter.load(rootsRepo.getRoots().sortedBy { it.name })
//        viewState.updateAdapter() //todo load?
    }

    fun quit(): Boolean {
        Log.d(ROOTS_SCREEN, "back clicked")
        router.exit()
        return true
    }

    inner class XItemGridPresenter :
        ItemGridPresenter<Unit, Path>({
            Log.d(ROOTS_SCREEN, "[mock] creating Tags screen with $it")
//            router.replaceScreen(Screens.TagsScreen(
//                resources = resourcesRepo.retrieveResources(root.folder)))

        }) {

        private var roots: List<Path> = listOf()

        override fun label() = Unit

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
}