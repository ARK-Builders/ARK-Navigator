package space.taran.arkbrowser.mvp.model.entity.common

import java.nio.file.Path

enum class Icon {
    FOLDER, PLUS, FILE
}

data class IconOrImage(val icon: Icon? = null, val image: Path? = null)