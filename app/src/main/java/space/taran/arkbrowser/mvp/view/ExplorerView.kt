package space.taran.arkbrowser.mvp.view

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType

@StateStrategyType(AddToEndSingleStrategy::class)
interface ExplorerView: MvpView {
    fun init()
    fun updateAdapter()
    fun setFavoriteFabVisibility(isVisible: Boolean)
    fun setTagsFabVisibility(isVisible: Boolean)
    fun showDialog()
    fun closeDialog()
    fun setTitle(title: String)
    @StateStrategyType(SkipStrategy::class)
    fun requestSdCardUri()
    @StateStrategyType(SkipStrategy::class)
    fun showToast(msg: String)
    @StateStrategyType(SkipStrategy::class)
    fun openFile(uri: String, mimeType: String)
    @StateStrategyType(SkipStrategy::class)
    fun setSelectedTab(pos: Int)
}
