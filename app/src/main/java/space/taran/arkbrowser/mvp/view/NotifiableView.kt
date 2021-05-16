package space.taran.arkbrowser.mvp.view

import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType

interface NotifiableView {

    @StateStrategyType(SkipStrategy::class)
    fun notifyUser(message: String, moreTime: Boolean = false)
}