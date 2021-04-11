package space.taran.arkbrowser.mvp.view

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType

@StateStrategyType(AddToEndSingleStrategy::class)
interface RootView: MvpView {
    fun init()
    fun updateRootAdapter()
    fun updateDialogAdapter()
    fun openChooserDialog()
    fun closeChooserDialog()
    @StateStrategyType(SkipStrategy::class)
    fun requestSdCardUri()
    fun setDialogPath(path: String)
}