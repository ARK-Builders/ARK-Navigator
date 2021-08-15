package space.taran.arknavigator.mvp.view.item

import space.taran.arknavigator.mvp.model.dao.common.Preview

interface FileItemView {

    fun position(): Int

    fun setIcon(icon: Preview)
    fun setText(title: String)

}
