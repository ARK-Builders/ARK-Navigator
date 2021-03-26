package space.taran.arkbrowser.mvp.presenter.adapter

import space.taran.arkbrowser.mvp.view.item.DetailItemView

interface IDetailListPresenter {
    fun getCount(): Int
    fun bindView(view: DetailItemView)
}