package space.taran.arknavigator.mvp.model.dao.common

import space.taran.arknavigator.utils.provideIconImage
import java.nio.file.Files
import java.nio.file.Path

enum class PredefinedIcon {
    FOLDER, FILE
    //todo: customs for HTML, PDF, MP3 and similar stuff
}

//todo: is it possible to use java.nio.Path here?
data class Preview(val predefined: PredefinedIcon? = null, val image: Path? = null) {
    companion object {
        fun provide(path: Path): Preview {
            if (Files.isDirectory(path)) {
                return Preview(predefined = PredefinedIcon.FOLDER)
            }

            val image = provideIconImage(path)
                ?: return Preview(predefined = PredefinedIcon.FILE)

            return Preview(image = image)
        }
    }
}