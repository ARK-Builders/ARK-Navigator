package space.taran.arkbrowser.mvp.view.item

import java.nio.file.Path

interface DetailItemView {
    var pos: Int

    fun setImage(file: Path)
}