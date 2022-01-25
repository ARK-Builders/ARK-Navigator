package space.taran.arknavigator.mvp.view

import java.nio.file.Path
import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.StateStrategyType

@StateStrategyType(AddToEndSingleStrategy::class)
interface FoldersView : MvpView, NotifiableView {
    fun init()
    fun setProgressVisibility(isVisible: Boolean, withText: String = "")
    fun openRootPickerDialog(paths: List<Path>)
    fun closeRootPickerDialog()
    fun updateFoldersTree()
    fun updateRootPickerDialogPath(path: Path)
    fun updateRootPickerDialogPickBtnState(isEnabled: Boolean, isRoot: Boolean)
}
