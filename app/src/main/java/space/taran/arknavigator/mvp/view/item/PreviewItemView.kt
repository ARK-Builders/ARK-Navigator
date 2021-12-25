package space.taran.arknavigator.mvp.view.item

import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import java.nio.file.Path

interface PreviewItemView {
    var pos: Int

    fun setSource(preview: Path?, placeholder: Int, resource: ResourceMeta)

    fun enableZoom()

    fun resetZoom()
}
