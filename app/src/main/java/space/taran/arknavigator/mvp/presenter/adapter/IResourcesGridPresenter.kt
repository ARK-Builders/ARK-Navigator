package space.taran.arknavigator.mvp.presenter.adapter

import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.view.item.FileItemView

interface IResourcesGridPresenter {
    fun getCount(): Int
    fun bindView(view: FileItemView)
    fun onItemClick(pos: Int)
}