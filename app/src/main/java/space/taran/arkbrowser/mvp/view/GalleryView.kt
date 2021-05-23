package space.taran.arkbrowser.mvp.view

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.StateStrategyType
import space.taran.arkbrowser.mvp.presenter.adapter.PreviewsList

@StateStrategyType(AddToEndSingleStrategy::class)
interface GalleryView: MvpView {
    fun init(previews: PreviewsList)
    fun setTitle(title: String)
}