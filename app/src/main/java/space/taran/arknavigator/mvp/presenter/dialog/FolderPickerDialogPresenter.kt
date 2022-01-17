package space.taran.arknavigator.mvp.presenter.dialog

import android.util.Log
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import space.taran.arknavigator.mvp.model.repo.FoldersRepo
import space.taran.arknavigator.mvp.presenter.adapter.FoldersGridPresenter
import space.taran.arknavigator.mvp.view.dialog.FolderPickerDialogView
import space.taran.arknavigator.utils.FOLDER_PICKER
import space.taran.arknavigator.utils.listDevices
import java.nio.file.Path
import javax.inject.Inject

class FolderPickerDialogPresenter(
    private val paths: List<Path>
): MvpPresenter<FolderPickerDialogView>() {

    @Inject
    lateinit var foldersRepo: FoldersRepo

    private var rootNotFavorite: Boolean = true
    private lateinit var roots: Set<Path>
    private lateinit var favorites: List<Path>
    private lateinit var devices: List<Path>
    private lateinit var currentFolder: Path

    val gridPresenter = FoldersGridPresenter(viewState, ::onFolderChanged)

    override fun onFirstViewAttach() {
        presenterScope.launch {
            devices = listDevices()
            viewState.init()
            val folders = foldersRepo.provideFolders().succeeded
            roots = folders.keys
            favorites = folders.values.flatten()
            gridPresenter.init(paths)
        }
    }

    fun onPickBtnClick() {
        if (!devices.contains(currentFolder)) {
            viewState.notifyPathPicked(currentFolder, rootNotFavorite)
        } else {
            Log.d(FOLDER_PICKER, "potentially huge directory")
            viewState.notifyDeviceChosenAsRoot()
        }
    }

    private fun onFolderChanged(folder: Path) = presenterScope.launch {
        currentFolder = folder
        viewState.setFolderName(currentFolder.toString())

        val rootPrefix = roots.find { currentFolder.startsWith(it) }
        if (rootPrefix != null) {
            if (rootPrefix == currentFolder) {
                rootNotFavorite = true
                viewState.setPickBtnState(isEnabled = false, rootNotFavorite)
            } else {
                var foundAsFavorite = false
                favorites.forEach {
                    if (currentFolder.endsWith(it)) {
                        foundAsFavorite = true
                        return@forEach
                    }
                }
                rootNotFavorite = false
                viewState.setPickBtnState(isEnabled = !foundAsFavorite, rootNotFavorite)
            }
        } else {
            rootNotFavorite = true
            viewState.setPickBtnState(isEnabled = true, rootNotFavorite)
        }
    }

    fun onBackClick(): Boolean {
        return gridPresenter.onBackClick()
    }
}