package space.taran.arknavigator.mvp.view.item

import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.preview.Preview
import java.nio.file.Path

interface FileItemView {
    fun position(): Int

    fun setFolderIcon()

    fun setGenericIcon(path: Path)

    fun setIconOrPreview(path: Path, preview: Preview)

    fun setText(title: String)
}
