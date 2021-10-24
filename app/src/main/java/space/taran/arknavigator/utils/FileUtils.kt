package space.taran.arknavigator.utils

import space.taran.arknavigator.ui.App
import java.io.File
import java.lang.IllegalArgumentException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.fileSize

val ROOT_PATH: Path = Paths.get("/")

val ANDROID_DIRECTORY: Path = Paths.get("Android")

typealias Milliseconds = Long
typealias StringPath = String

enum class Sorting {
    DEFAULT, NAME, SIZE, LAST_MODIFIED, TYPE
}

enum class FileActionType {
    EDIT_AS_OPEN, EDIT_AND_OPEN, OPEN_ONLY, OPEN_ONLY_DETACH_PROCESS
}

const val PDF_PREVIEW_FOLDER_NAME = "pdf_preview"

private val acceptedImageExt = listOf("jpg", "jpeg", "png")
private val acceptedVideoExt = listOf("mp4", "avi", "mov", "wmv", "flv")
private val acceptedEditOnlyExt = arrayListOf("txt", "doc", "docx", "odt", "ods")
    .also { it.addAll(acceptedImageExt) }
private val acceptedReadAndEditExt = listOf("pdf", "md")

fun isImage(filePath: Path?): Boolean {
    val extension = extension(filePath)
    return acceptedImageExt.contains(extension)
}

fun isVideo(filePath: Path?): Boolean {
    return if (filePath?.toFile()?.exists() == true && filePath.fileSize() > 0) {
        val extension = extension(filePath)
        acceptedVideoExt.contains(extension)
    } else {
        false
    }
}

fun isFormat(filePath: Path?, ext: String): Boolean {
    return extension(filePath) == ext
}

fun getPdfPreviewsFolder(): File =
    Paths.get("${App.instance.cacheDir}/$PDF_PREVIEW_FOLDER_NAME").toFile()

fun getSavedPdfPreviews(): List<String?>? =
    getPdfPreviewsFolder()
        .listFiles()
        ?.map {
            it.nameWithoutExtension
        }

fun getFileActionType(filePath: Path): FileActionType {
    return when (extension(filePath)) {
        in acceptedEditOnlyExt -> FileActionType.EDIT_AS_OPEN
        in acceptedReadAndEditExt -> FileActionType.EDIT_AND_OPEN
        in acceptedVideoExt -> FileActionType.OPEN_ONLY_DETACH_PROCESS
        else -> FileActionType.OPEN_ONLY
    }
}

fun isHidden(path: Path): Boolean =
    path.fileName.toString().startsWith('.')

fun folderExists(path: Path): Boolean =
    Files.exists(path) || Files.isDirectory(path)

fun listDevices(): List<Path> =
    App.instance.getExternalFilesDirs(null)
        .toList()
        .filterNotNull()
        .map {
            it.toPath().toRealPath()
                .takeWhile { part ->
                    part != ANDROID_DIRECTORY
                }
                .fold(ROOT_PATH) { parent, child ->
                    parent.resolve(child)
                }
        }

fun findLongestCommonPrefix(paths: List<Path>): Path {
    if (paths.isEmpty()) {
        throw IllegalArgumentException("Can't search for common prefix among empty collection")
    }

    if (paths.size == 1) {
        return paths.first()
    }

    fun tailrec(_prefix: Path, paths: List<Path>): Pair<Path, List<Path>> {
        val grouped = paths.groupBy { it.getName(0) }
        if (grouped.size > 1) {
            return _prefix to paths
        }

        val prefix = _prefix.resolve(grouped.keys.first())
        val shortened = grouped.values.first()
            .map { prefix.relativize(it) }

        return tailrec(prefix, shortened)
    }

    return tailrec(ROOT_PATH, paths).first
}

fun deleteFile(filePath: Path?) {
    val file = filePath?.toFile()
    if (file?.exists() == true) {
        file.delete()
    }
}

fun extension(path: Path?): String {
    return path?.extension?.lowercase() ?: ""
}

fun reifySorting(sorting: Sorting): Comparator<Path>? =
    when (sorting) {
        Sorting.NAME -> compareBy { it.fileName }
        Sorting.SIZE -> compareBy { Files.size(it) }
        Sorting.TYPE -> compareBy { it.fileName.extension }
        Sorting.LAST_MODIFIED -> compareBy { Files.getLastModifiedTime(it) }
        Sorting.DEFAULT -> null
    }

const val KILOBYTE = 1024
const val MEGABYTE = 1024 * KILOBYTE