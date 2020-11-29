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

    fun setCurrentItem(pos: Int)
    fun updateAdapter()
}