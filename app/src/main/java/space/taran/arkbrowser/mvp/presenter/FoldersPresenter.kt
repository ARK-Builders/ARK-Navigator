package space.taran.arkbrowser.mvp.presenter

import android.util.Log
import space.taran.arkbrowser.mvp.view.FoldersView
import moxy.InjectViewState
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import space.taran.arkbrowser.mvp.model.repo.Folders
import space.taran.arkbrowser.utils.CoroutineRunner.runAndBlock
import space.taran.arkbrowser.mvp.model.repo.FoldersRepo
import space.taran.arkbrowser.mvp.model.repo.ResourcesIndexFactory
import space.taran.arkbrowser.ui.fragments.utils.Notifications
import space.taran.arkbrowser.utils.FOLDERS_SCREEN
import java.lang.AssertionError
import java.lang.IllegalStateException
import java.nio.file.Path
import javax.inject.Inject

//todo: protect foldersRepo when enabling real concurrency

@InjectViewState
class FoldersPresenter: MvpPresenter<FoldersView>() {
    @Inject
    lateinit var router: Router

    @Inject
    lateinit var foldersRepo: FoldersRepo

    @Inject
    lateinit var resourcesIndexexRepo: ResourcesIndexFactory

    private lateinit var rootToFavorites: MutableMap<Path, MutableList<Path>>

    override fun onFirstViewAttach() {
        Log.d(FOLDERS_SCREEN, "first view attached in RootsPresenter")
        super.onFirstViewAttach()

        val result: Folders = runAndBlock {
            val result = foldersRepo.query()

            Notifications.notifyIfFailedPaths(viewState, result.failed)
            return@runAndBlock result.succeeded
        }
        Log.d(FOLDERS_SCREEN, "folders loaded: $result")

        rootToFavorites = result
            .mapValues { (_, favorites) -> favorites.toMutableList() }
            .toMutableMap()

        viewState.loadFolders(rootToFavorites)
    }

    fun addRoot(root: Path) {
        Log.d(FOLDERS_SCREEN, "root $root added in RootsPresenter")
        val path = root.toRealPath()

        if (rootToFavorites.containsKey(path)) {
            throw AssertionError("Path must be checked in RootPicker")
        }

        rootToFavorites[path] = mutableListOf()

        runAndBlock {
            foldersRepo.insertRoot(path)
        }

        viewState.notifyUser(
            message = "indexing of huge folders can take minutes",
            moreTime = true)
        //todo: non-blocking indexing
        resourcesIndexexRepo.buildFromFilesystem(root)

        viewState.loadFolders(rootToFavorites)
    }

    fun addFavorite(favorite: Path) {
        Log.d(FOLDERS_SCREEN, "favorite $favorite added in RootsPresenter")
        val path = favorite.toRealPath()

        val root = rootToFavorites.keys.find { path.startsWith(it) }
            ?: throw IllegalStateException("Can't add favorite if it's root is not added")

        val relative = root.relativize(path)
        if (rootToFavorites[root]!!.contains(relative)) {
            throw AssertionError("Path must be checked in RootPicker")
        }

        rootToFavorites[root]!!.add(relative)

        runAndBlock {
            foldersRepo.insertFavorite(root, relative)
        }

        viewState.loadFolders(rootToFavorites)
    }

    fun resume() {
        Log.d(FOLDERS_SCREEN, "view resumed in RootsPresenter")
        viewState.loadFolders(rootToFavorites)
    }

    fun quit(): Boolean {
        Log.d(FOLDERS_SCREEN, "back clicked")
        router.exit()
        return true
    }
}