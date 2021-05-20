package space.taran.arkbrowser.mvp.view.item

import java.nio.file.Path

interface PreviewItemView {
    var pos: Int

    fun setImage(file: Path)
}