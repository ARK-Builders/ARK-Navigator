package space.taran.arknavigator.mvp.view

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.StateStrategyType
import space.taran.arknavigator.mvp.presenter.TagsSelector
import space.taran.arknavigator.utils.Sorting

@StateStrategyType(AddToEndSingleStrategy::class)
interface ResourcesView: MvpView, NotifiableView {
    fun init()
    fun updateAdapter()
    fun setProgressVisibility(isVisible: Boolean)
    fun setToolbarTitle(title: String)
    fun setTagsEnabled(enabled: Boolean)
    fun drawChips(tagsSelector: TagsSelector?)
    fun setSortDialogVisibility(isVisible: Boolean, sorting: Sorting, ascending: Boolean)
}