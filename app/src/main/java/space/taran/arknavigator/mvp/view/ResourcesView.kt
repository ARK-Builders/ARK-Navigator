package space.taran.arknavigator.mvp.view

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.StateStrategyType

@StateStrategyType(AddToEndSingleStrategy::class)
interface ResourcesView : MvpView, NotifiableView {
    fun init()
    fun updateAdapter()
    fun setProgressVisibility(isVisible: Boolean, withText: String = "")
    fun setToolbarTitle(title: String)
    fun setKindTagsEnabled(enabled: Boolean)
    fun updateMenu()
    fun setTagsSelectorHintEnabled(enabled: Boolean)
    fun setTagsFilterEnabled(enabled: Boolean)
    fun setTagsFilterText(filter: String)
    fun drawTags()
}
