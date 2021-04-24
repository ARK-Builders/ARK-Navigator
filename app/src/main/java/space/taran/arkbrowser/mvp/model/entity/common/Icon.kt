package space.taran.arkbrowser.mvp.model.entity.common

import java.io.File

enum class Icon {
    FOLDER, PLUS, FILE, ROOT
}

data class IconOrImage(val icon: Icon? = null, val image: File? = null)