package com.taran.imagemanager.mvp.presenter.adapter

import com.taran.imagemanager.mvp.view.item.DetailItemView

interface IDetailListPresenter {
    fun getCount(): Int
    fun bindView(view: DetailItemView)
}