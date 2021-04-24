package space.taran.arkbrowser.mvp.view.item

import space.taran.arkbrowser.mvp.model.entity.common.IconOrImage

interface FileItemView {
    var pos: Int

    fun setIcon(icon: IconOrImage)
    fun setText(title: String)
    fun setFav(isFav: Boolean)
}
