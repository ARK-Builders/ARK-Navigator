package com.taran.imagemanager.mvp.view

import com.taran.imagemanager.mvp.model.entity.Folder
import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType

@StateStrategyType(AddToEndSingleStrategy::class)
interface ExplorerView: MvpView {
    fun init()
    fun updateAdapter()
    fun setFabVisibility(isVisible: Boolean)

    @StateStrategyType(SkipStrategy::class)
    fun showDialog()

    @StateStrategyType(SkipStrategy::class)
    fun requestSdCardUri()
}