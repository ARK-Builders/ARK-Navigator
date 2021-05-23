package space.taran.arkbrowser.mvp.model.entity.common

import space.taran.arkbrowser.utils.provideIconImage
import java.nio.file.Files
import java.nio.file.Path

enum class PredefinedIcon {
    FOLDER, FILE
    //todo: customs for HTML, PDF, MP3 and similar stuff
}

//todo: is it possible to use java.nio.Path here?
data class Icon(val predefined: PredefinedIcon? = null, val image: Path? = null) {
    companion object {
        fun provide(path: Path): Icon {
            if (Files.isDirectory(path)) {
                return Icon(predefined = PredefinedIcon.FOLDER)
            }

            val image = provideIconImage(path)
                ?: return Icon(predefined = PredefinedIcon.FILE)

            return Icon(image = image)
        }
    }
}