package space.taran.arknavigator.ui.fragments.utils

import space.taran.arknavigator.utils.FileType
import space.taran.arknavigator.utils.provideIconImage
import java.nio.file.Files
import java.nio.file.Path

enum class PredefinedIcon {
    FOLDER, FILE
}

data class Preview(
    val predefinedFolder: PredefinedIcon? = null,
    val previewPath: Path? = null,
    val fileType: FileType? = FileType.UNDEFINED,
    val fileExtension: String? = null,
    val extraInfo: MutableMap<ExtraInfoTag, String>? = null
) {
    companion object {
        fun provide(path: Path): Preview {
            if (Files.isDirectory(path)) {
                return Preview(predefinedFolder = PredefinedIcon.FOLDER)
            }

            val previewFile = provideIconImage(path)

            return Preview(
                previewPath = previewFile.file,
                fileType = previewFile.fileType,
                fileExtension = previewFile.fileExtension,
                extraInfo = previewFile.extraInfo
            )
        }
    }

    enum class ExtraInfoTag {
        MEDIA_RESOLUTION, MEDIA_DURATION
    }
}