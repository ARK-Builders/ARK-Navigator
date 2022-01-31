package space.taran.arknavigator.mvp.view.dialog

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.StateStrategyType
import space.taran.arknavigator.utils.Sorting

@StateStrategyType(AddToEndSingleStrategy::class)
interface SortDialogView : MvpView {
    fun init(sorting: Sorting, ascending: Boolean)
    fun closeDialog()
}
