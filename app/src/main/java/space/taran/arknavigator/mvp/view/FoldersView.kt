package space.taran.arknavigator.mvp.view

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.StateStrategyType
import space.taran.arknavigator.mvp.model.repo.Folders
import java.nio.file.Path

@StateStrategyType(AddToEndSingleStrategy::class)
interface FoldersView: MvpView, NotifiableView {
    fun init()
    fun setProgressVisibility(isVisible: Boolean)
    fun openRootPickerDialog(paths: List<Path>)
    fun closeRootPickerDialog()
    fun updateFoldersTree(devices: List<Path>, folders: Folders)
    fun updateRootPickerDialogPath(path: Path)
    fun updateRootPickerDialogPickBtnState(isEnabled: Boolean, isRoot: Boolean)
}