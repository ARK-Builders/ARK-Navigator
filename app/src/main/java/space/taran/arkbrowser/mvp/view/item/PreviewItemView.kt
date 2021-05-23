package space.taran.arkbrowser.mvp.view.item

import space.taran.arkbrowser.mvp.model.dao.common.PredefinedIcon
import java.nio.file.Path

interface PreviewItemView {
    var pos: Int

    fun setImage(file: Path)

    fun setPredefined(resource: PredefinedIcon)
}