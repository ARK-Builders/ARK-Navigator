package space.taran.arknavigator.ui.adapter.previewpager

import space.taran.arklib.domain.index.Resource
import space.taran.arklib.domain.preview.PreviewAndThumbnail
import java.nio.file.Path

interface PreviewItemView {
    var pos: Int

    fun reset()
    fun setSource(
        source: Path,
        resource: Resource,
        previewAndThumbnail: PreviewAndThumbnail?
    )
}
