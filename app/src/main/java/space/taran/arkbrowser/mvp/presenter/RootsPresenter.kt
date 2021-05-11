package space.taran.arkbrowser.mvp.presenter

import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import space.taran.arkbrowser.mvp.view.RootView
import moxy.InjectViewState
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import space.taran.arkbrowser.mvp.model.repo.Folders
import space.taran.arkbrowser.mvp.model.repo.FoldersRepo
import space.taran.arkbrowser.utils.CONCURRENT
import space.taran.arkbrowser.utils.ROOTS_SCREEN
import java.lang.AssertionError
import java.nio.file.Path
import javax.inject.Inject

@InjectViewState
class RootsPresenter: MvpPresenter<RootView>() {
    @Inject
    lateinit var router: Router

    @Inject
    lateinit var foldersRepo: FoldersRepo

    private lateinit var folders: Folders

    override fun onFirstViewAttach() {
        Log.d(ROOTS_SCREEN, "first view attached in RootsPresenter")
        super.onFirstViewAttach()

        Log.d(CONCURRENT, "runBlocking[-1]")
        runBlocking {
            Log.d(CONCURRENT, "runBlocking[0]")
            //            GlobalScope.launch {
            launch {
                Log.d(CONCURRENT, "launch[0]")
                val result = foldersRepo.query()
                //todo protect `folders`?

                if (result.failed.isNotEmpty()) {
                    viewState.notifyUser(
                "Failed to load the following roots:\n" +
                        result.failed.joinToString("\n"))
                }

                folders = result.succeeded
                Log.d(CONCURRENT, "launch[1]")
            }
            Log.d(CONCURRENT, "runBlocking[1]")
        }
        Log.d(CONCURRENT, "runBlocking[2]")

        Log.d(ROOTS_SCREEN, "folders loaded: $folders")
        viewState.loadFolders(folders)

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
        Log.d(ROOTS_SCREEN, "root $path added in RootsPresenter")

        if (folders.containsKey(path)) {
            throw AssertionError("Path must be checked in RootPicker")
        }

        folders = folders.plus(path to listOf())
        //todo persist

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

        viewState.loadFolders(folders)
    }

    fun resume() {
        Log.d(ROOTS_SCREEN, "view resumed in RootsPresenter")
        viewState.loadFolders(folders)
    }

    fun quit(): Boolean {
        Log.d(ROOTS_SCREEN, "back clicked")
        router.exit()
        return true
    }
}