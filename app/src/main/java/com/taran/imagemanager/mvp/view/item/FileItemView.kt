package space.taran.arkbrowser.mvp.view.item

import space.taran.arkbrowser.mvp.model.entity.Icons

interface FileItemView {
    var pos: Int

    fun setIcon(resourceType: Icons, path: String?)
    fun setText(title: String)
}