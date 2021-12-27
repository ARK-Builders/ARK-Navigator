package space.taran.arknavigator.mvp.presenter

import android.util.Log
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import space.taran.arknavigator.R
import space.taran.arknavigator.mvp.model.repo.FoldersRepo
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndexRepo
import space.taran.arknavigator.mvp.presenter.adapter.ItemClickHandler
import space.taran.arknavigator.mvp.presenter.adapter.folderstree.FoldersTreePresenter
import space.taran.arknavigator.mvp.view.FoldersView
import space.taran.arknavigator.navigation.AppRouter
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.ui.fragments.utils.Notifications
import space.taran.arknavigator.ui.resource.StringProvider
import space.taran.arknavigator.utils.FOLDERS_SCREEN
import space.taran.arknavigator.utils.FOLDER_PICKER
import space.taran.arknavigator.utils.listDevices
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject

class FoldersPresenter : MvpPresenter<FoldersView>() {
    @Inject
    lateinit var router: AppRouter

    @Inject
    lateinit var foldersRepo: FoldersRepo

    @Inject
    lateinit var resourcesIndexRepo: ResourcesIndexRepo

    @Inject
    lateinit var stringProvider: StringProvider

    var foldersTreePresenter = FoldersTreePresenter(viewState, ::onFoldersTreeAddFavoriteBtnClick)
        .apply {
            App.instance.appComponent.inject(this)
        }

    private lateinit var devices: List<Path>

    private var favoritesByRoot: MutableMap<Path, MutableList<Path>> = mutableMapOf()
        set(value) {
            field = value
            roots = value.keys
        }

    private lateinit var roots: Set<Path>
    private var rootNotFavorite: Boolean = true

    override fun onFirstViewAttach() {
        Log.d(FOLDERS_SCREEN, "first view attached in RootsPresenter")
        super.onFirstViewAttach()

        viewState.init()
        presenterScope.launch {
            viewState.setProgressVisibility(true, "Loading")
            val folders = foldersRepo.provideFolders()
            devices = listDevices()

            Notifications.notifyIfFailedPaths(viewState, folders.failed)

            favoritesByRoot = folders.succeeded
                .mapValues { (_, favorites) -> favorites.toMutableList() }
                .toMutableMap()

            foldersTreePresenter.updateNodes(devices, favoritesByRoot)
            viewState.setProgressVisibility(false)
        }
    }

    fun onFoldersTreeAddFavoriteBtnClick(path: Path) {
        viewState.openRootPickerDialog(listOf(path))
    }

    fun onAddRootBtnClick() {
        viewState.openRootPickerDialog(devices)
    }

    fun onRootPickerItemClick(): ItemClickHandler<Path> = { _, path ->
        Log.d(FOLDER_PICKER, "path $path was clicked")

        if (Files.isDirectory(path)) {
            viewState.updateRootPickerDialogPath(path)
            updateFolderAndButtonState(path)
        } else {
            Log.d(FOLDER_PICKER, "but it is not a directory")
            viewState.notifyUser(stringProvider.getString(R.string.folders_file_chosen_as_root))
        }
    }

    private fun updateFolderAndButtonState(path: Path) {
        val rootPrefix = roots.find { path.startsWith(it) }

        if (rootPrefix != null) {
            if (rootPrefix == path) {
                viewState.updateRootPickerDialogPickBtnState(isEnabled = false, isRoot = true)
                rootNotFavorite = true
            } else {
                var foundAsFavorite = false
                getFavorites().forEach {
                    if (path.endsWith(it)) {
                        foundAsFavorite = true
                    }
                }
                viewState.updateRootPickerDialogPickBtnState(isEnabled = !foundAsFavorite, isRoot = false)
                rootNotFavorite = false
            }
        } else {
            viewState.updateRootPickerDialogPickBtnState(isEnabled = true, isRoot = true)
            rootNotFavorite = true
        }
    }

    fun onRootPickerCancelClick() {
        viewState.closeRootPickerDialog()
    }

    fun onRootPickerBackClick() {
        viewState.closeRootPickerDialog()
    }

    fun onPickRootBtnClick(path: Path) {
        if (!devices.contains(path)) {
            if (rootNotFavorite) {
                // adding path as root
                if (roots.contains(path)) {
                    viewState.notifyUser(stringProvider.getString(R.string.folders_root_is_already_picked))
                } else {
                    addRoot(path)
                    viewState.closeRootPickerDialog()
                }
            } else {
                // adding path as favorite
                if (getFavorites().contains(path)) {
                    viewState.notifyUser(stringProvider.getString(R.string.folders_favorite_is_alreay_picked))
                } else {
                    addFavorite(path)
                    viewState.closeRootPickerDialog()
                }
            }
        } else {
            Log.d(FOLDER_PICKER, "potentially huge directory")
            viewState.notifyUser(stringProvider.getString(R.string.folders_device_chosen_as_root))
        }
    }

    private fun addRoot(root: Path) = presenterScope.launch(NonCancellable) {
        viewState.setProgressVisibility(true, "Adding folder")
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

        viewState.setProgressVisibility(true, "Indexing")
        resourcesIndexRepo.buildFromFilesystem(root)
        viewState.setProgressVisibility(false)

        foldersTreePresenter.updateNodes(devices, favoritesByRoot)
    }

    private fun getFavorites(): Set<Path> = favoritesByRoot.values.flatten().toSet()

    private fun addFavorite(favorite: Path) = presenterScope.launch(NonCancellable) {
        viewState.setProgressVisibility(true, "Adding folder")
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

        foldersTreePresenter.updateNodes(devices, favoritesByRoot)
        viewState.setProgressVisibility(false)
    }

    fun navigateBackClick(path: Path?) {
        Log.d(FOLDERS_SCREEN, "[back] clicked, path: $path")
        if (path != null) updateFolderAndButtonState(path)
    }


    fun onBackClick() {
        Log.d(FOLDERS_SCREEN, "[back] clicked")
        router.exit()
    }
}
