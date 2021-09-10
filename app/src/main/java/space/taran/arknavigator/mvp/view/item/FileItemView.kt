package space.taran.arknavigator.mvp.view.item

import space.taran.arknavigator.ui.fragments.utils.Preview

interface FileItemView {

    fun position(): Int

    fun setIcon(icon: Preview)
    fun setText(title: String)

}
