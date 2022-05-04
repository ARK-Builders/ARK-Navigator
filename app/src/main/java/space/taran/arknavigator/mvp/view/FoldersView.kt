package space.taran.arknavigator.mvp.view

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType
import java.nio.file.Path

@StateStrategyType(AddToEndSingleStrategy::class)
interface FoldersView : MvpView, NotifiableView {
    fun init()
    fun setProgressVisibility(isVisible: Boolean, withText: String = "")
    fun updateFoldersTree()

    @StateStrategyType(SkipStrategy::class)
    fun openRootPickerDialog(paths: List<Path>)

    @StateStrategyType(SkipStrategy::class)
    fun openRootsScanDialog()
}
