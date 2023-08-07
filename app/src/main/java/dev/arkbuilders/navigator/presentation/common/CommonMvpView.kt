package dev.arkbuilders.navigator.presentation.common

import moxy.MvpView
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType
import java.nio.file.Path

interface CommonMvpView : MvpView {
    @StateStrategyType(SkipStrategy::class)
    fun toastIndexFailedPath(path: Path)
}
