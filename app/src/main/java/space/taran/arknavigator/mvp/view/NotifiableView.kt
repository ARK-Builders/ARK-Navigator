package space.taran.arknavigator.mvp.view

import androidx.annotation.StringRes
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType

interface NotifiableView {

    @StateStrategyType(SkipStrategy::class)
    fun notifyUser(message: String, moreTime: Boolean = false)

    @StateStrategyType(SkipStrategy::class)
    fun notifyUser(@StringRes messageID: Int, moreTime: Boolean = false)
}
