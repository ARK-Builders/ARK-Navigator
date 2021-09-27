package space.taran.arknavigator.mvp.view.item

import space.taran.arknavigator.ui.fragments.utils.PredefinedIcon
import java.nio.file.Path

interface PreviewItemView {
    var pos: Int

    fun setImage(file: Path, playVisible: Boolean = false)

    fun setPDFPreview(file: Path)

    fun setPredefined(resource: PredefinedIcon)

    fun setZoomEnabled(enabled: Boolean)

    fun resetZoom()
}