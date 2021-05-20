package space.taran.arkbrowser.mvp.view.item

import space.taran.arkbrowser.mvp.model.entity.common.Icon

interface FileItemView {

    fun position(): Int

    fun setIcon(icon: Icon)
    fun setText(title: String)

}
