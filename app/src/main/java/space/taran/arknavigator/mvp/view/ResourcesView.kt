package space.taran.arknavigator.mvp.view

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.StateStrategyType
import space.taran.arknavigator.mvp.presenter.adapter.ResourcesList

@StateStrategyType(AddToEndSingleStrategy::class)
interface ResourcesView: MvpView, NotifiableView {
    fun init(grid: ResourcesList)

    fun setToolbarTitle(title: String)
}