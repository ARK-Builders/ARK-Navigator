package space.taran.arknavigator.utils.extensions

import space.taran.arknavigator.ui.App
import space.taran.arknavigator.utils.PDF_PREVIEW_FOLDER_NAME
import java.nio.file.Path
import java.nio.file.Paths

fun String?.toPdfPreviewPath(): Path =
    Paths.get("${App.instance.cacheDir}/$PDF_PREVIEW_FOLDER_NAME/${this}.png")