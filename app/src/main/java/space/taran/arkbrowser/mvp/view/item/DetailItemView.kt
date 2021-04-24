package space.taran.arkbrowser.mvp.view.item

import java.io.File

interface DetailItemView {
    var pos: Int

    fun setImage(file: File)
}