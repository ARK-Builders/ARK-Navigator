package dev.arkbuilders.navigator.presentation.screen.main

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType

@StateStrategyType(AddToEndSingleStrategy::class)
interface MainView : MvpView {
    fun init()

    @StateStrategyType(SkipStrategy::class)
    fun requestPerms()

    fun enterResourceFragmentFailed()
}