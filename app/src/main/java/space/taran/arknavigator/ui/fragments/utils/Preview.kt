package space.taran.arknavigator.ui.fragments.utils

import space.taran.arknavigator.utils.FileType
import space.taran.arknavigator.utils.provideIconImage
import java.nio.file.Files
import java.nio.file.Path

enum class PredefinedIcon {
    FOLDER, FILE
}

data class Preview(
    val predefined: PredefinedIcon? = null,
    val image: Path? = null,
    val fileType: FileType? = FileType.UNDEFINED
) {
    companion object {
        fun provide(path: Path): Preview {
            if (Files.isDirectory(path)) {
                return Preview(predefined = PredefinedIcon.FOLDER)
            }

            val filePair = provideIconImage(path)
            val image = filePair?.second
                ?: return Preview(predefined = PredefinedIcon.FILE, fileType = FileType.UNDEFINED)

            return Preview(image = image, fileType = filePair.first)
        }
    }
}