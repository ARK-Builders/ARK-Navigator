package dev.arkbuilders.navigator.presentation.dialog.sort

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.StateStrategyType
import dev.arkbuilders.navigator.data.utils.Sorting

@StateStrategyType(AddToEndSingleStrategy::class)
interface SortDialogView : MvpView {
    fun init(sorting: Sorting, ascending: Boolean, sortByScoresEnabled: Boolean)
    fun closeDialog()
}
