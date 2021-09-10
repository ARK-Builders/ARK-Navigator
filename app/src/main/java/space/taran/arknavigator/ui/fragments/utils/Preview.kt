package space.taran.arknavigator.ui.fragments.utils

import space.taran.arknavigator.utils.provideIconImage
import java.nio.file.Files
import java.nio.file.Path

enum class PredefinedIcon {
    FOLDER, FILE
}

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