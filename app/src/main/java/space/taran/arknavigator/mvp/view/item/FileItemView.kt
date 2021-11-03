package space.taran.arknavigator.mvp.view.item

import space.taran.arknavigator.mvp.model.repo.ResourceMeta
import space.taran.arknavigator.ui.fragments.utils.Preview
import java.nio.file.Path

interface FileItemView {

    fun position(): Int

    fun setFolderIcon()

    fun setGenericIcon(path: Path)

    fun setIconOrPreview(path: Path, meta: ResourceMeta)

    fun setText(title: String)

}
