package dev.arkbuilders.navigator.presentation.screen.folders

import android.util.Log
import dev.arkbuilders.navigator.data.preferences.PreferenceKey
import dev.arkbuilders.navigator.data.preferences.Preferences
import dev.arkbuilders.navigator.data.utils.listDevices
import dev.arkbuilders.navigator.di.modules.RepoModule.Companion.MESSAGE_FLOW_NAME
import dev.arkbuilders.navigator.presentation.navigation.AppRouter
import dev.arkbuilders.navigator.presentation.navigation.Screens
import dev.arkbuilders.navigator.presentation.utils.StringProvider
import dev.arkbuilders.navigator.data.utils.LogTags.FOLDERS_SCREEN
import dev.arkbuilders.navigator.data.utils.LogTags.FOLDERS_TREE
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import space.taran.arkfilepicker.folders.FoldersRepo
import space.taran.arkfilepicker.folders.RootAndFav
import space.taran.arkfilepicker.presentation.folderstree.DeviceNode
import space.taran.arkfilepicker.presentation.folderstree.FavoriteNode
import space.taran.arkfilepicker.presentation.folderstree.FolderNode
import space.taran.arkfilepicker.presentation.folderstree.RootNode
import space.taran.arklib.domain.Message
import space.taran.arklib.domain.index.ResourceIndexRepo
import space.taran.arklib.domain.preview.PreviewProcessorRepo
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Named

class FoldersPresenter(
    private val rescanRoots: Boolean
) : MvpPresenter<FoldersView>() {
    @Inject
    lateinit var router: AppRouter

    @Inject
    lateinit var foldersRepo: FoldersRepo

    @Inject
    lateinit var resourcesIndexRepo: ResourceIndexRepo

    @Inject
    lateinit var previewsStorageRepo: PreviewProcessorRepo

    @Inject
    lateinit var stringProvider: StringProvider

    @Inject
    lateinit var preferences: Preferences

    @Inject
    @Named(MESSAGE_FLOW_NAME)
    lateinit var messageFlow: MutableSharedFlow<Message>

    private lateinit var devices: List<Path>

    override fun onFirstViewAttach() {
        Log.d(FOLDERS_SCREEN, "first view attached in RootsPresenter")
        super.onFirstViewAttach()

        viewState.init()
        presenterScope.launch {
            viewState.setProgressVisibility(true, "Loading folders list")
            val folders = foldersRepo.provideWithMissing()
            devices = listDevices()

            viewState.toastFailedPath(folders.failed)

            viewState.updateFoldersTree(devices, folders.succeeded)
            viewState.setProgressVisibility(false)

            messageFlow.onEach { message ->
                when (message) {
                    is Message.KindDetectFailed -> viewState.toastIndexFailedPath(
                        message.path
                    )
                }
            }.launchIn(presenterScope)

            if (rescanRoots) {
                viewState.openRootsScanDialog()
                return@launch
            }

            if (!preferences.get(PreferenceKey.WasRootsScanShown) &&
                folders.succeeded.keys.isEmpty()
            ) {
                preferences.set(PreferenceKey.WasRootsScanShown, true)
                viewState.openRootsScanDialog()
            }
        }
    }

    fun onNavigateBtnClick(node: FolderNode) {
        when (node) {
            is DeviceNode -> {}
            is RootNode -> {
                router.navigateTo(
                    Screens.ResourcesScreen(
                        RootAndFav(node.path.toString(), null)
                    )
                )
            }
            is FavoriteNode -> {
                router.navigateTo(
                    Screens.ResourcesScreen(
                        RootAndFav(node.root.toString(), node.path.toString())
                    )
                )
            }
        }
    }

    fun onFoldersTreeAddFavoriteBtnClick(node: FolderNode) {
        viewState.openRootPickerDialog(node.path)
    }

    fun onAddRootBtnClick() {
        viewState.openRootPickerDialog(null)
    }

    fun onPickRootBtnClick(path: Path, rootNotFavorite: Boolean) =
        presenterScope.launch(NonCancellable) {
            val folders = foldersRepo.provideFolders()

            if (rootNotFavorite) {
                // adding path as root
                if (folders.keys.contains(path)) {
                    viewState.toastRootIsAlreadyPicked()
                } else {
                    addRoot(path)
                }
            } else {
                // adding path as favorite
                if (folders.values.flatten().contains(path)) {
                    viewState.toastFavoriteIsAlreadyPicked()
                } else {
                    addFavorite(path)
                }
            }
        }

    fun onRootsFound(roots: List<Path>) = presenterScope.launch(NonCancellable) {
        roots.forEach { root ->
            foldersRepo.addRoot(root)
        }
        viewState.updateFoldersTree(devices, foldersRepo.provideFolders())
    }

    fun onForgetBtnClick(node: FolderNode) {
        viewState.openConfirmForgetFolderDialog(node)
    }

    fun onForgetRoot(root: Path, deleteFromMemory: Boolean) =
        presenterScope.launch(NonCancellable) {
            viewState.setProgressVisibility(true, "Forgetting root folder")
            if (deleteFromMemory) {
                Log.d(
                    FOLDERS_TREE,
                    "forgetting and deleting root folder $root"
                )
                foldersRepo.deleteRoot(root)
            } else {
                Log.d(
                    FOLDERS_TREE,
                    "forgetting root folder $root"
                )
                foldersRepo.forgetRoot(root)
            }
            viewState.updateFoldersTree(devices, foldersRepo.provideFolders())
            viewState.setProgressVisibility(false)
        }

    fun onForgetFavorite(root: Path, favorite: Path, deleteFromMemory: Boolean) =
        presenterScope.launch(NonCancellable) {
            viewState.setProgressVisibility(true, "Forgetting favorite folder")
            if (deleteFromMemory) {
                Log.d(
                    FOLDERS_TREE,
                    "forgetting and deleting favorite $favorite"
                )
                foldersRepo.deleteFavorite(root, favorite)
            } else {
                Log.d(
                    FOLDERS_TREE,
                    "forgetting favorite $favorite"
                )
                foldersRepo.forgetFavorite(root, favorite)
            }
            viewState.updateFoldersTree(devices, foldersRepo.provideFolders())
            viewState.setProgressVisibility(false)
        }

    private suspend fun addRoot(root: Path) {
        viewState.setProgressVisibility(true, "Adding root folder")
        Log.d(FOLDERS_SCREEN, "root $root added in RootsPresenter")
        val path = root.toRealPath()
        val folders = foldersRepo.provideFolders()

        if (folders.containsKey(path)) {
            throw AssertionError("Path must be checked in RootPicker")
        }

        foldersRepo.addRoot(path)

        viewState.toastIndexingCanTakeMinutes()

        viewState.setProgressVisibility(true, "Providing root index")
        // any valid root folder must contain `.ark` subfolder
        val index = resourcesIndexRepo.provide(root)
        index.updateAll()
        viewState.setProgressVisibility(false)

        viewState.updateFoldersTree(devices, foldersRepo.provideFolders())
    }

    private fun addFavorite(favorite: Path) =
        presenterScope.launch(NonCancellable) {
            viewState.setProgressVisibility(true, "Adding favorite folder")
            Log.d(FOLDERS_SCREEN, "favorite $favorite added in RootsPresenter")
            val path = favorite.toRealPath()
            val folders = foldersRepo.provideFolders()

            val root = folders.keys.find { path.startsWith(it) }
                ?: throw IllegalStateException(
                    "Can't add favorite if it's root is not added"
                )

            val relative = root.relativize(path)
            if (folders[root]!!.contains(relative)) {
                throw AssertionError("Path must be checked in RootPicker")
            }

            foldersRepo.addFavorite(root, relative)

            viewState.updateFoldersTree(devices, foldersRepo.provideFolders())
            viewState.setProgressVisibility(false)
        }

    fun onBackClick() {
        Log.d(FOLDERS_SCREEN, "[back] clicked")
        router.exit()
    }
}
