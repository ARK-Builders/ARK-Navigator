package space.taran.arkbrowser.mvp.view.item

import space.taran.arkbrowser.mvp.model.entity.common.Icons

interface FileItemView {
    var pos: Int

    fun setIcon(resourceType: Icons, path: String?)
    fun setText(title: String)
    fun setFav()
}
