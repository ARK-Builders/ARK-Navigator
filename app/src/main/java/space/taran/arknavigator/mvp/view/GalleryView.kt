package space.taran.arknavigator.mvp.view

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.StateStrategyType
import space.taran.arknavigator.mvp.presenter.adapter.PreviewsList

@StateStrategyType(AddToEndSingleStrategy::class)
interface GalleryView: MvpView {
    fun init(previews: PreviewsList)
    fun setTitle(title: String)
    fun setFullscreen(fullscreen: Boolean)
    fun setPreviewsScrollingEnabled(enabled: Boolean)
    fun openResourceDetached(pos: Int)
}