package dev.arkbuilders.navigator.presentation.screen.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dev.arkbuilders.arkfilepicker.folders.FoldersRepo
import dev.arkbuilders.arklib.data.index.ResourceIndexRepo
import dev.arkbuilders.navigator.analytics.folders.FoldersAnalytics
import dev.arkbuilders.navigator.data.PermissionsHelper
import dev.arkbuilders.navigator.data.preferences.PreferenceKey
import dev.arkbuilders.navigator.data.preferences.Preferences
import dev.arkbuilders.navigator.data.utils.DevicePathsExtractor
import dev.arkbuilders.navigator.data.utils.LogTags
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import timber.log.Timber
import java.nio.file.Path
import javax.inject.Inject

class ProgressWithText(val enabled: Boolean, val text: String = "")

data class FoldersState private constructor(
    val initialized: Boolean,
    val devices: List<Path>,
    val folders: Map<Path, List<Path>>,
    val progressWithText: ProgressWithText
) {
    companion object {
        fun initial() =
            FoldersState(
                initialized = false,
                devices = emptyList(),
                folders = emptyMap(),
                progressWithText = ProgressWithText(false)
            )
    }
}

sealed class FoldersSideEffect {
    object OpenRootsScanDialog : FoldersSideEffect()
    object ShowExplainPermsDialog : FoldersSideEffect()
    class ToastFailedPaths(val failedPaths: List<Path>) : FoldersSideEffect()
    object ToastRootIsAlreadyPicked : FoldersSideEffect()
    object ToastFavoriteIsAlreadyPicked : FoldersSideEffect()
    object ToastIndexingCanTakeMinutes : FoldersSideEffect()
}

class FoldersViewModel @Inject constructor(
    private val rescanRoots: Boolean,
    private val foldersRepo: FoldersRepo,
    private val resourcesIndexRepo: ResourceIndexRepo,
    private val preferences: Preferences,
    private val permsHelper: PermissionsHelper,
    private val devicePathsExtractor: DevicePathsExtractor,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val foldersAnalytics: FoldersAnalytics
) : ViewModel(), ContainerHost<FoldersState, FoldersSideEffect> {

    override val container: Container<FoldersState, FoldersSideEffect> = container(
        FoldersState.initial()
    )

    private lateinit var devices: List<Path>

    init {
        foldersAnalytics.trackScreen()
        intent {
            if (!permsHelper.isWritePermissionGranted()) {
                var granted =
                    permsHelper.askForWritePermissionsAndAwait(viewModelScope)
                while (!granted) {
                    granted = permsHelper.customAskAndAwait(viewModelScope) {
                        postSideEffect(FoldersSideEffect.ShowExplainPermsDialog)
                    }
                }
            }

            reduce {
                state.copy(
                    progressWithText =
                    ProgressWithText(true, "Loading folders list")
                )
            }
            val folders = foldersRepo.provideWithMissing()
            devices = devicePathsExtractor.listDevices()

            postSideEffect(FoldersSideEffect.ToastFailedPaths(folders.failed))

            reduce {
                state.copy(
                    initialized = true,
                    devices = devices,
                    folders = folders.succeeded,
                    progressWithText = ProgressWithText(false)
                )
            }

            showRootsScanIfNeeded()
        }
    }

    fun onRootsFound(roots: List<Path>) = intent {
        roots.forEach { root ->
            foldersRepo.addRoot(root)
        }
        val folders = foldersRepo.provideFolders()
        reduce {
            state.copy(folders = folders)
        }
    }

    fun onForgetRoot(root: Path, deleteFromMemory: Boolean) = intent {
        reduce {
            state.copy(
                progressWithText =
                ProgressWithText(true, "Forgetting root folder")
            )
        }

        if (deleteFromMemory) {
            Timber.d(
                LogTags.FOLDERS_TREE,
                "forgetting and deleting root folder $root"
            )
            foldersRepo.deleteRoot(root)
        } else {
            Timber.d(
                LogTags.FOLDERS_TREE,
                "forgetting root folder $root"
            )
            foldersRepo.forgetRoot(root)
        }
        val folders = foldersRepo.provideFolders()
        reduce {
            state.copy(
                folders = folders,
                progressWithText = ProgressWithText(false)
            )
        }
    }

    fun onForgetFavorite(
        root: Path,
        favorite: Path,
        deleteFromMemory: Boolean
    ) = intent {
        reduce {
            state.copy(
                progressWithText =
                ProgressWithText(true, "Forgetting favorite folder")
            )
        }
        if (deleteFromMemory) {
            Timber.d(
                LogTags.FOLDERS_TREE,
                "forgetting and deleting favorite $favorite"
            )
            foldersRepo.deleteFavorite(root, favorite)
        } else {
            Timber.d(
                LogTags.FOLDERS_TREE,
                "forgetting favorite $favorite"
            )
            foldersRepo.forgetFavorite(root, favorite)
        }
        val folders = foldersRepo.provideFolders()
        reduce {
            state.copy(
                folders = folders,
                progressWithText = ProgressWithText(false)
            )
        }
    }

    fun onPickRootBtnClick(path: Path, rootNotFavorite: Boolean) = intent {
        val folders = foldersRepo.provideFolders()

        if (rootNotFavorite) {
            if (folders.keys.contains(path)) {
                postSideEffect(FoldersSideEffect.ToastRootIsAlreadyPicked)
            } else {
                addRoot(path)
            }
        } else {
            if (folders.values.flatten().contains(path)) {
                postSideEffect(FoldersSideEffect.ToastFavoriteIsAlreadyPicked)
            } else {
                addFavorite(path)
            }
        }
    }

    private suspend fun addRoot(root: Path) = intent {
        reduce {
            state.copy(
                progressWithText =
                ProgressWithText(true, "Adding root folder")
            )
        }

        Timber.d(LogTags.FOLDERS_SCREEN, "root $root added in RootsPresenter")
        val path = withContext(defaultDispatcher) {
            root.toRealPath()
        }
        var folders = foldersRepo.provideFolders()

        if (folders.containsKey(path)) {
            throw AssertionError("Path must be checked in RootPicker")
        }

        foldersAnalytics.trackRootAdded()
        foldersRepo.addRoot(path)

        postSideEffect(FoldersSideEffect.ToastIndexingCanTakeMinutes)

        reduce {
            state.copy(
                progressWithText =
                ProgressWithText(true, "Providing root index")
            )
        }

        // any valid root folder must contain `.ark` subfolder
        val index = resourcesIndexRepo.provide(root)
        index.updateAll()
        folders = foldersRepo.provideFolders()
        reduce {
            state.copy(
                folders = folders,
                progressWithText = ProgressWithText(false)
            )
        }
    }

    private fun addFavorite(favorite: Path) = intent {
        reduce {
            state.copy(
                progressWithText =
                ProgressWithText(true, "Adding favorite folder")
            )
        }
        Timber.d(
            LogTags.FOLDERS_SCREEN,
            "favorite $favorite added in RootsPresenter"
        )
        val path = withContext(defaultDispatcher) {
            favorite.toRealPath()
        }
        var folders = foldersRepo.provideFolders()

        val root = folders.keys.find { path.startsWith(it) }
            ?: throw IllegalStateException(
                "Can't add favorite if it's root is not added"
            )

        val relative = root.relativize(path)
        if (folders[root]?.contains(relative) == true) {
            throw AssertionError("Path must be checked in RootPicker")
        }

        foldersAnalytics.trackFavAdded()
        foldersRepo.addFavorite(root, relative)
        folders = foldersRepo.provideFolders()
        reduce {
            state.copy(
                folders = folders,
                progressWithText = ProgressWithText(false)
            )
        }
    }

    private fun showRootsScanIfNeeded() = intent {
        if (rescanRoots) {
            postSideEffect(FoldersSideEffect.OpenRootsScanDialog)
            return@intent
        }

        val folders = foldersRepo.provideFolders()

        if (!preferences.get(PreferenceKey.WasRootsScanShown) &&
            folders.keys.isEmpty()
        ) {
            preferences.set(PreferenceKey.WasRootsScanShown, true)
            postSideEffect(FoldersSideEffect.OpenRootsScanDialog)
        }
    }
}

class FoldersViewModelFactory @AssistedInject constructor(
    @Assisted private val rescanRoots: Boolean,
    private val foldersRepo: FoldersRepo,
    private val resourcesIndexRepo: ResourceIndexRepo,
    private val preferences: Preferences,
    private val permsHelper: PermissionsHelper,
    private val devicePathsExtractor: DevicePathsExtractor,
    private val foldersAnalytics: FoldersAnalytics
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FoldersViewModel(
            rescanRoots = rescanRoots,
            foldersRepo = foldersRepo,
            resourcesIndexRepo = resourcesIndexRepo,
            preferences = preferences,
            permsHelper = permsHelper,
            devicePathsExtractor = devicePathsExtractor,
            foldersAnalytics = foldersAnalytics
        ) as T
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted rescanRoots: Boolean
        ): FoldersViewModelFactory
    }
}
