package space.taran.arknavigator.mvp.view

import java.nio.file.Path
import moxy.MvpView
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType

interface CommonMvpView : MvpView {
    @StateStrategyType(SkipStrategy::class)
    fun toastIndexFailedPath(path: Path)
}
