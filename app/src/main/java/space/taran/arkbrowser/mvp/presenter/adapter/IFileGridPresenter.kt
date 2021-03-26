package space.taran.arkbrowser.mvp.presenter.adapter

import space.taran.arkbrowser.mvp.view.item.FileItemView

interface IFileGridPresenter {
    fun getCount(): Int
    fun bindView(view: FileItemView)
    fun onCardClicked(pos: Int)
}