package space.taran.arknavigator.ui.fragments.utils

import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.dao.computeId
import space.taran.arknavigator.utils.*
import java.nio.file.Files
import java.nio.file.Path

enum class PredefinedIcon {
    FOLDER, FILE
}

data class Preview(
    val predefinedIcon: PredefinedIcon? = null,
    val previewPath: Path? = null,
    val fileType: FileType? = FileType.UNDEFINED,
    val fileExtension: String? = null
) {
    val extraInfo by lazy {
        if (previewPath != null && isVideo(previewPath))
            getVideoInfo(previewPath)
        else null
    }
    companion object {
        fun provide(path: Path, resourceID: ResourceId? = null): Preview {
            if (Files.isDirectory(path)) {
                return Preview(predefinedIcon = PredefinedIcon.FOLDER)
            }

            return provideIconImage(path, resourceID)
        }

        fun createPair(file: Path, resourceID: ResourceId?): Preview {
            return when {
                isImage(file) -> {
                    Preview(
                        fileType = FileType.IMAGE,
                        previewPath = file,
                        fileExtension = extensionWithoutDot(file)
                    )
                }
                isVideo(file) -> {
                    Preview(
                        fileType = FileType.VIDEO,
                        previewPath =file,
                        fileExtension = extensionWithoutDot(file),
                        //extraInfo = getVideoInfo(file)
                    )
                }
                isPDF(file) -> getPdfPreview(file, resourceID)
                isFormat(file, ".gif") ->
                    Preview(
                        fileType = FileType.GIF,
                        previewPath = file,
                        fileExtension = extensionWithoutDot(file)
                    )
                else -> Preview(fileExtension = extensionWithoutDot(file))
            }
        }

        private fun getPdfPreview(file: Path, resourceID: ResourceId? = null): Preview {
            val id = resourceID?: computeId(file)
            val savedPreviews = getSavedPdfPreviews()
            return if (savedPreviews?.contains(id) == true) {
                Preview(fileType = FileType.PDF, previewPath = getPdfPreviewByID(id), fileExtension = extension(file))
            } else Preview(fileType = FileType.PDF, previewPath = file, fileExtension = extension(file))
        }
    }

    enum class ExtraInfoTag {
        MEDIA_RESOLUTION, MEDIA_DURATION
    }
}