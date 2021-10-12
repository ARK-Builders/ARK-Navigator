package space.taran.arknavigator.mvp.model.repo

import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.dao.computeId
import space.taran.arknavigator.ui.fragments.utils.PredefinedIcon
import space.taran.arknavigator.ui.fragments.utils.Preview
import space.taran.arknavigator.utils.*
import java.nio.file.Files
import java.nio.file.Path

class PreviewRepo {

    fun providePreview(path: Path, resourceID: ResourceId? = null): Preview {
        if (Files.isDirectory(path)) {
            return Preview(predefinedIcon = PredefinedIcon.FOLDER)
        }

        return createPair(path, resourceID)
    }

    private fun createPair(file: Path, resourceID: ResourceId?): Preview {
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