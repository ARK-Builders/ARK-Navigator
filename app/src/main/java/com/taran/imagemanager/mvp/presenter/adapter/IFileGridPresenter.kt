package com.taran.imagemanager.mvp.presenter.adapter

import com.taran.imagemanager.mvp.view.item.FileItemView

interface IFileGridPresenter {
    fun getCount(): Int
    fun bindView(view: FileItemView)
    fun onCardClicked(pos: Int)
}