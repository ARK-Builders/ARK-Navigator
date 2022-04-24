package space.taran.arknavigator.mvp.presenter

import android.util.Log
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import space.taran.arknavigator.R
import space.taran.arknavigator.mvp.model.repo.FoldersRepo
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndexRepo
import space.taran.arknavigator.mvp.presenter.adapter.folderstree.FoldersTreePresenter
import space.taran.arknavigator.mvp.view.FoldersView
import space.taran.arknavigator.navigation.AppRouter
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.ui.fragments.utils.Notifications
import space.taran.arknavigator.ui.resource.StringProvider
import space.taran.arknavigator.utils.LogTags.FOLDERS_SCREEN
import space.taran.arknavigator.utils.listDevices
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

    var foldersTreePresenter = FoldersTreePresenter(
        viewState,
        ::onFoldersTreeAddFavoriteBtnClick
    )
        .apply {
            App.instance.appComponent.inject(this)
        }

    private lateinit var devices: List<Path>

    override fun onFirstViewAttach() {
        Log.d(FOLDERS_SCREEN, "first view attached in RootsPresenter")
        super.onFirstViewAttach()

        viewState.init()
        presenterScope.launch {
            viewState.setProgressVisibility(true, "Loading")
            val folders = foldersRepo.provideFoldersWithMissing()
            devices = listDevices()

            Notifications.notifyIfFailedPaths(viewState, folders.failed)

            foldersTreePresenter.updateNodes(devices, folders.succeeded)
            viewState.setProgressVisibility(false)
        }
    }

    fun onFoldersTreeAddFavoriteBtnClick(path: Path) {
        viewState.openRootPickerDialog(listOf(path))
    }

    fun onAddRootBtnClick() {
        viewState.openRootPickerDialog(devices)
    }

    fun onPickRootBtnClick(path: Path, rootNotFavorite: Boolean) =
        presenterScope.launch(NonCancellable) {
            val folders = foldersRepo.provideFolders()

            if (rootNotFavorite) {
                // adding path as root
                if (folders.keys.contains(path)) {
                    viewState.notifyUser(
                        stringProvider
                            .getString(R.string.folders_root_is_already_picked)
                    )
                } else {
                    addRoot(path)
                }
            } else {
                // adding path as favorite
                if (folders.values.flatten().contains(path)) {
                    viewState.notifyUser(
                        stringProvider.getString(
                            R.string.folders_favorite_is_alreay_picked
                        )
                    )
                } else {
                    addFavorite(path)
                }
            }
        }

    private fun addRoot(root: Path) =
        presenterScope.launch(NonCancellable) {
            viewState.setProgressVisibility(true, "Adding folder")
            Log.d(FOLDERS_SCREEN, "root $root added in RootsPresenter")
            val path = root.toRealPath()
            val folders = foldersRepo.provideFolders()

            if (folders.containsKey(path)) {
                throw AssertionError("Path must be checked in RootPicker")
            }

            foldersRepo.insertRoot(path)

            viewState.notifyUser(
                message = "Indexing of huge folders can take minutes",
                moreTime = true
            )

            viewState.setProgressVisibility(true, "Indexing")
            resourcesIndexRepo.buildFromFilesystem(root)
            viewState.setProgressVisibility(false)

            foldersTreePresenter.updateNodes(devices, foldersRepo.provideFolders())
        }

    private fun addFavorite(favorite: Path) =
        presenterScope.launch(NonCancellable) {
            viewState.setProgressVisibility(true, "Adding folder")
            Log.d(FOLDERS_SCREEN, "favorite $favorite added in RootsPresenter")
            val path = favorite.toRealPath()
            val folders = foldersRepo.provideFolders()

            val root = folders.keys.find { path.startsWith(it) }
                ?: throw IllegalStateException(
                    "Can't add favorite if it's root is not added"
                )

            val relative = root.relativize(path)
            if (folders.values.flatten().contains(relative)) {
                throw AssertionError("Path must be checked in RootPicker")
            }

            foldersRepo.insertFavorite(root, relative)

            foldersTreePresenter.updateNodes(devices, foldersRepo.provideFolders())
            viewState.setProgressVisibility(false)
        }

    fun onBackClick() {
        Log.d(FOLDERS_SCREEN, "[back] clicked")
        router.exit()
    }
}
