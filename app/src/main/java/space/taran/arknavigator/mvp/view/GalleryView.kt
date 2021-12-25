package space.taran.arknavigator.mvp.view

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType
import space.taran.arknavigator.mvp.presenter.adapter.PreviewsList

@StateStrategyType(AddToEndSingleStrategy::class)
interface GalleryView : MvpView {
    fun init(previews: PreviewsList)
    @StateStrategyType(SkipStrategy::class)
    fun showEditTagsDialog(position: Int)
    fun setTitle(title: String)
    fun setFullscreen(fullscreen: Boolean)
    fun setPreviewsScrollingEnabled(enabled: Boolean)
    fun viewInExternalApp(pos: Int)
}
