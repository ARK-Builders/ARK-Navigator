package com.taran.imagemanager.mvp.view

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType

@StateStrategyType(AddToEndSingleStrategy::class)
interface ExplorerView: MvpView {
    fun init()

    @StateStrategyType(SkipStrategy::class)
    fun updateAdapter()

    @StateStrategyType(SkipStrategy::class)
    fun setFabVisibility(isVisible: Boolean)

    @StateStrategyType(SkipStrategy::class)
    fun showDialog()
}