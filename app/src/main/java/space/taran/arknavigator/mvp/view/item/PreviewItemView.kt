package space.taran.arknavigator.mvp.view.item

import space.taran.arknavigator.mvp.model.repo.ResourceMetaExtra
import java.nio.file.Path

interface PreviewItemView {
    var pos: Int

    fun setSource(preview: Path?, placeholder: Int, extra: ResourceMetaExtra?)

    fun enableZoom()

    fun resetZoom()
}