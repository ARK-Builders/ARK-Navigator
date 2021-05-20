package space.taran.arkbrowser.mvp.presenter.adapter

import space.taran.arkbrowser.mvp.view.item.PreviewItemView

interface IDetailListPresenter {
    fun getCount(): Int
    fun bindView(view: PreviewItemView)
}