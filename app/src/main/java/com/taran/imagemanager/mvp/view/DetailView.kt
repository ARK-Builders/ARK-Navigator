package com.taran.imagemanager.mvp.view

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType

@StateStrategyType(AddToEndSingleStrategy::class)
interface DetailView: MvpView {
    fun init()

    @StateStrategyType(SkipStrategy::class)
    fun showTagsDialog(tags: String)

    @StateStrategyType(SkipStrategy::class)
    fun setCurrentItem(pos: Int)

    @StateStrategyType(SkipStrategy::class)
    fun updateAdapter()
}