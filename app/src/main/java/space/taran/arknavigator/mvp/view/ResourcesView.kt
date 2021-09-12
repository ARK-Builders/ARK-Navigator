package space.taran.arknavigator.mvp.view

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.StateStrategyType
import space.taran.arknavigator.utils.Sorting

@StateStrategyType(AddToEndSingleStrategy::class)
interface ResourcesView: MvpView, NotifiableView {
    fun init()
    fun updateAdapter()
    fun setProgressVisibility(isVisible: Boolean)
    fun setToolbarTitle(title: String)
    fun setTagsEnabled(enabled: Boolean)
    fun setTagsSelectorHintEnabled(enabled: Boolean)
    fun drawTags()
    fun showSortDialog(sorting: Sorting, ascending: Boolean)
    fun closeSortDialog()
}