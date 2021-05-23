package space.taran.arkbrowser.mvp.view

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.StateStrategyType
import space.taran.arkbrowser.mvp.presenter.ResourcesGrid

@StateStrategyType(AddToEndSingleStrategy::class)
interface ResourcesView: MvpView, NotifiableView {
    fun init(grid: ResourcesGrid)

    fun setToolbarTitle(title: String)
}

//todo why do we need these interfaces at all?