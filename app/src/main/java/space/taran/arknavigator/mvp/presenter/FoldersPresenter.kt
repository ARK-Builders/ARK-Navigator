package space.taran.arknavigator.mvp.presenter

import android.util.Log
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import space.taran.arknavigator.mvp.view.FoldersView
import moxy.MvpPresenter
import moxy.presenterScope
import ru.terrakok.cicerone.Router
import space.taran.arknavigator.mvp.model.repo.FoldersRepo
import space.taran.arknavigator.mvp.model.repo.ResourcesIndexFactory
import space.taran.arknavigator.mvp.presenter.adapter.ItemClickHandler
import space.taran.arknavigator.ui.fragments.utils.Notifications
import space.taran.arknavigator.utils.FOLDERS_SCREEN
import space.taran.arknavigator.utils.FOLDER_PICKER
import space.taran.arknavigator.utils.listDevices
import java.lang.AssertionError
import java.lang.IllegalStateException
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject

//todo: protect foldersRepo when enabling real concurrency

class FoldersPresenter : MvpPresenter<FoldersView>() {
    @Inject
    lateinit var router: Router

    @Inject
    lateinit var foldersRepo: FoldersRepo

    @Inject
    lateinit var resourcesIndexFactory: ResourcesIndexFactory

    private lateinit var devices: List<Path>
    //todo treat syncthing folder as special storage device

    private var favoritesByRoot: MutableMap<Path, MutableList<Path>> = mutableMapOf()
        set(value) {
            field = value
            roots = value.keys
            favorites = value.values.flatten().toSet()
        }

    private lateinit var roots: Set<Path>
    private lateinit var favorites: Set<Path>
    private var rootNotFavorite: Boolean = true

    override fun onFirstViewAttach() {
        Log.d(FOLDERS_SCREEN, "first view attached in RootsPresenter")
        super.onFirstViewAttach()

        viewState.init()
        presenterScope.launch {
            viewState.setProgressVisibility(true)
            val folders = foldersRepo.query()
            Log.d(FOLDERS_SCREEN, "folders retrieved: $folders")
            devices = listDevices()

            Notifications.notifyIfFailedPaths(viewState, folders.failed)

            favoritesByRoot = folders.succeeded
                .mapValues { (_, favorites) -> favorites.toMutableList() }
                .toMutableMap()

            viewState.updateFoldersTree(devices, favoritesByRoot)
            viewState.setProgressVisibility(false)
        }
    }

    fun onFoldersTreeAddFavoriteBtnClick(path: Path) {
        viewState.setRootPickerDialogVisibility(listOf(path))
    }

    fun onAddRootBtnClick() {
        viewState.setRootPickerDialogVisibility(devices)
    }

    fun onRootPickerItemClick(): ItemClickHandler<Path> = { _, path ->
        Log.d(FOLDER_PICKER, "path $path was clicked")

        if (Files.isDirectory(path)) {
            viewState.updateRootPickerDialogPath(path)

            val rootPrefix = roots.find { path.startsWith(it) }
            if (rootPrefix != null) {
                if (rootPrefix == path) {
                    viewState.updateRootPickerDialogPickBtnState(isEnabled = false, isRoot = true)
                    rootNotFavorite = true
                } else {
                    viewState.updateRootPickerDialogPickBtnState(isEnabled = true, isRoot = false)
                    rootNotFavorite = false
                }
            } else {
                viewState.updateRootPickerDialogPickBtnState(isEnabled = true, isRoot = true)
                rootNotFavorite = true
            }
        } else {
            Log.d(FOLDER_PICKER, "but it is not a directory")
            //           notifyUser(FoldersFragment.FILE_CHOSEN_AS_ROOT)
        }
    }

    fun onRootPickerCancelClick() {
        viewState.setRootPickerDialogVisibility(null)
    }

    fun onRootPickerBackClick() {
        //todo: the business logic of dialog back click should be here
        // e.g. viewState.updateRootPickerDialog(newPath)
        // now this method is only called if the root picker cannot handle the back click
        viewState.setRootPickerDialogVisibility(null)
    }

    fun onPickRootBtnClick(path: Path) {
        if (!devices.contains(path)) {
            if (rootNotFavorite) {
                // adding path as root
                if (roots.contains(path)) {
                    //notifyUser(FoldersFragment.ROOT_IS_ALREADY_PICKED)
                } else {
                    addRoot(path)
                    viewState.setRootPickerDialogVisibility(null)
                }
            } else {
                // adding path as favorite
                if (favorites.contains(path)) {
                    //notifyUser(FoldersFragment.FAVORITE_IS_ALREADY_PICKED)
                } else {
                    addFavorite(path)
                    viewState.setRootPickerDialogVisibility(null)
                }
            }
        } else {
            Log.d(FOLDER_PICKER, "potentially huge directory")
            //notifyUser(FoldersFragment.DEVICE_CHOSEN_AS_ROOT)
        }
    }

    fun addRoot(root: Path) = presenterScope.launch(NonCancellable) {
        viewState.setProgressVisibility(true)
        Log.d(FOLDERS_SCREEN, "root $root added in RootsPresenter")
        val path = root.toRealPath()

        if (favoritesByRoot.containsKey(path)) {
            throw AssertionError("Path must be checked in RootPicker")
        }

        favoritesByRoot[path] = mutableListOf()

        foldersRepo.insertRoot(path)

        viewState.notifyUser(
            message = "Indexing of huge folders can take minutes",
            moreTime = true)

        resourcesIndexFactory.buildFromFilesystem(root)

        viewState.updateFoldersTree(devices, favoritesByRoot)
        viewState.setProgressVisibility(false)
    }

    fun addFavorite(favorite: Path) = presenterScope.launch(NonCancellable) {
        viewState.setProgressVisibility(true)
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

        viewState.updateFoldersTree(devices, favoritesByRoot)
        viewState.setProgressVisibility(false)
    }

    fun onBackClick(): Boolean {
        Log.d(FOLDERS_SCREEN, "[back] clicked")
        router.exit()
        return true
    }
}