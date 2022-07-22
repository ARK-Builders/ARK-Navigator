package space.taran.arknavigator.mvp.view.item

import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import java.nio.file.Path

interface FileItemView {
    fun position(): Int

    fun setFolderIcon()

    fun setGenericIcon(path: Path)

    fun reset(isSelectingEnabled: Boolean, isItemSelected: Boolean)
    fun setSelected(isItemSelected: Boolean)

    fun setIconOrPreview(path: Path, resource: ResourceMeta)

    fun setText(title: String)
}
