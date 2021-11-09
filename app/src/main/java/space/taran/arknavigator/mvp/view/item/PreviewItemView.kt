package space.taran.arknavigator.mvp.view.item

import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.preview.Preview
import java.nio.file.Path

interface PreviewItemView {
    var pos: Int

    fun setSource(preview: Preview, placeholder: Int)

    fun enableZoom()

    fun resetZoom()
}