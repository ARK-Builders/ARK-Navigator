package space.taran.arknavigator.mvp.view.item

import java.nio.file.Path
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta

interface FileItemView {
    fun position(): Int

    fun setFolderIcon()

    fun setGenericIcon(path: Path)

    fun setIconOrPreview(path: Path, resource: ResourceMeta)

    fun setText(title: String)
}
