package space.taran.arkbrowser.mvp.presenter

import android.util.Log
import space.taran.arkbrowser.mvp.view.FoldersView
import moxy.InjectViewState
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import space.taran.arkbrowser.mvp.model.repo.FoldersRepo
import space.taran.arkbrowser.mvp.model.repo.ResourcesIndexFactory
import space.taran.arkbrowser.ui.fragments.utils.Notifications
import space.taran.arkbrowser.utils.FOLDERS_SCREEN
import java.lang.AssertionError
import java.lang.IllegalStateException
import java.nio.file.Path
import javax.inject.Inject

//todo: protect foldersRepo when enabling real concurrency

//todo @InjectViewState ?
class FoldersPresenter: MvpPresenter<FoldersView>() {
    @Inject
    lateinit var router: Router

    @Inject
    lateinit var foldersRepo: FoldersRepo

    @Inject
    lateinit var resourcesIndexFactory: ResourcesIndexFactory

    private lateinit var favoritesByRoot: MutableMap<Path, MutableList<Path>>

    override fun onFirstViewAttach() {
        Log.d(FOLDERS_SCREEN, "first view attached in RootsPresenter")
        super.onFirstViewAttach()

        val folders = foldersRepo.query()
        Log.d(FOLDERS_SCREEN, "folders retrieved: $folders")

        Notifications.notifyIfFailedPaths(viewState, folders.failed)

        favoritesByRoot = folders.succeeded
            .mapValues { (_, favorites) -> favorites.toMutableList() }
            .toMutableMap()

        viewState.loadFolders(favoritesByRoot)
    }

    fun addRoot(root: Path) {
        Log.d(FOLDERS_SCREEN, "root $root added in RootsPresenter")
        val path = root.toRealPath()

        if (favoritesByRoot.containsKey(path)) {
            throw AssertionError("Path must be checked in RootPicker")
        }

        favoritesByRoot[path] = mutableListOf()

        foldersRepo.insertRoot(path)

        viewState.notifyUser(
            message = "indexing of huge folders can take minutes",
            moreTime = true)
        //todo: non-blocking indexing
        resourcesIndexFactory.buildFromFilesystem(root)

        viewState.loadFolders(favoritesByRoot)
    }

    fun addFavorite(favorite: Path) {
        Log.d(FOLDERS_SCREEN, "favorite $favorite added in RootsPresenter")
        val path = favorite.toRealPath()

        val root = favoritesByRoot.keys.find { path.startsWith(it) }
            ?: throw IllegalStateException("Can't add favorite if it's root is not added")

        val relative = root.relativize(path)
        if (favoritesByRoot[root]!!.contains(relative)) {
            throw AssertionError("Path must be checked in RootPicker")
        }

        favoritesByRoot[root]!!.add(relative)

        foldersRepo.insertFavorite(root, relative)

        viewState.loadFolders(favoritesByRoot)
    }

    fun resume() {
        Log.d(FOLDERS_SCREEN, "view resumed in RootsPresenter")
        viewState.loadFolders(favoritesByRoot)
    }

    fun quit(): Boolean {
        Log.d(FOLDERS_SCREEN, "[back] clicked")
        router.exit()
        return true
    }
}