package space.taran.arknavigator.mvp.view.item

import space.taran.arknavigator.mvp.model.dao.common.PredefinedIcon
import java.nio.file.Path

interface PreviewItemView {
    var pos: Int

    fun setImage(file: Path)

    fun setPredefined(resource: PredefinedIcon)
}