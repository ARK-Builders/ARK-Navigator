package space.taran.arknavigator.ui.fragments.utils

import space.taran.arknavigator.utils.FileType
import space.taran.arknavigator.utils.provideIconImage
import java.nio.file.Files
import java.nio.file.Path

enum class PredefinedIcon {
    FOLDER, FILE, PDF, DOCX, DOC, GIF, HTML, TXT, ODT, ODS, XLS, XLSX
}

data class Preview(
    val predefined: PredefinedIcon? = null,
    val previewPath: Path? = null,
    val fileType: FileType? = FileType.UNDEFINED,
    val extraInfo: MutableMap<ExtraInfoTag, String>? = null
) {
    companion object {
        fun provide(path: Path): Preview {
            if (Files.isDirectory(path)) {
                return Preview(predefined = PredefinedIcon.FOLDER)
            }

            val previewFile = provideIconImage(path)
                ?: return Preview(predefined = PredefinedIcon.FILE, fileType = FileType.UNDEFINED)

            if (previewFile.predefinedIcon != null)
                return Preview(
                    predefined = previewFile.predefinedIcon,
                    fileType = FileType.UNDEFINED)

            val filePath = previewFile.file
            return Preview(
                previewPath = filePath,
                fileType = previewFile.fileType,
                extraInfo = previewFile.extraInfo
            )
        }
    }

    enum class ExtraInfoTag {
        MEDIA_RESOLUTION, MEDIA_DURATION
    }
}