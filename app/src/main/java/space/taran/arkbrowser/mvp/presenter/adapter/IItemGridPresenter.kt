package space.taran.arkbrowser.mvp.presenter.adapter

import space.taran.arkbrowser.mvp.view.item.FileItemView

interface IItemGridPresenter {
    fun getCount(): Int
    fun bindView(view: FileItemView)

    fun itemClicked(pos: Int)
}