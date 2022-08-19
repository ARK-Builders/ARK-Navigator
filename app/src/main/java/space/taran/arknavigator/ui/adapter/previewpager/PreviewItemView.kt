package space.taran.arknavigator.ui.adapter.previewpager

import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.preview.PreviewAndThumbnail
import java.nio.file.Path

interface PreviewItemView {
    var pos: Int

    fun reset()
    fun setSource(
        source: Path,
        meta: ResourceMeta,
        previewAndThumbnail: PreviewAndThumbnail?
    )
}
