package space.taran.arknavigator.mvp.view

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.StateStrategyType
import space.taran.arknavigator.mvp.presenter.adapter.PreviewsList
import space.taran.arknavigator.mvp.presenter.adapter.ResourcesList

@StateStrategyType(AddToEndSingleStrategy::class)
interface GalleryView: MvpView {
    fun init(previews: PreviewsList)
    fun setTitle(title: String)
    fun setFullscreen(fullscreen: Boolean)
    fun setViewPagerInputEnabled(enabled: Boolean)
}