package space.taran.arkbrowser.mvp.view

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType
import java.nio.file.Path

@StateStrategyType(AddToEndSingleStrategy::class)
interface RootView: MvpView {
    fun init()
    fun updateAdapter()

    fun openRootPicker(paths: List<Path>, handler: (Path) -> Unit)

    @StateStrategyType(SkipStrategy::class)
    fun requestSdCardUri()
}