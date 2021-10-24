package space.taran.arknavigator.mvp.view.item

import space.taran.arknavigator.mvp.model.repo.ResourceMeta
import space.taran.arknavigator.ui.fragments.utils.Preview

interface FileItemView {

    fun position(): Int

    fun setIcon(icon: Preview, meta: ResourceMeta?)
    fun setText(title: String)

}
