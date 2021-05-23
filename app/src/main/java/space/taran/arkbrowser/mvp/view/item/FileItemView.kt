package space.taran.arkbrowser.mvp.view.item

import space.taran.arkbrowser.mvp.model.dao.common.Preview

interface FileItemView {

    fun position(): Int

    fun setIcon(icon: Preview)
    fun setText(title: String)

}
