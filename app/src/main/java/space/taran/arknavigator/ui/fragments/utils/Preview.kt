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

    enum class ExtraInfoTag {
        MEDIA_RESOLUTION, MEDIA_DURATION
    }
}