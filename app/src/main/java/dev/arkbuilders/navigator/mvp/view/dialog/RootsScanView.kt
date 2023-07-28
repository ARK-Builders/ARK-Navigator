package dev.arkbuilders.navigator.mvp.view.dialog

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.OneExecutionStateStrategy
import moxy.viewstate.strategy.StateStrategyType
import java.nio.file.Path

@StateStrategyType(AddToEndSingleStrategy::class)
interface RootsScanView : MvpView {
    fun init()
    fun startScan()
    fun scanCompleted(foundRoots: Int)
    fun setProgress(foundRoots: Int)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun notifyRootsFound(roots: List<Path>)
    @StateStrategyType(OneExecutionStateStrategy::class)
    fun toastFolderSkip(folder: Path)
    @StateStrategyType(OneExecutionStateStrategy::class)
    fun closeDialog()
}
