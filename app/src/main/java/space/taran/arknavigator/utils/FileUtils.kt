package space.taran.arknavigator.utils

import space.taran.arknavigator.ui.App
import java.lang.IllegalArgumentException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension

val ROOT_PATH: Path = Paths.get("/")

val ANDROID_DIRECTORY: Path = Paths.get("Android")

typealias Milliseconds = Long
typealias StringPath = String

enum class Sorting {
    DEFAULT, NAME, SIZE, LAST_MODIFIED, TYPE
}

enum class FileActionType{
    EDIT_AS_OPEN, EDIT_AND_OPEN, OPEN_ONLY
}

private val acceptedImageExt = listOf(".jpg", ".jpeg", ".png")
private val acceptedEditOnlyExt = arrayListOf(".txt", ".doc", ".docx", ".odt", "ods")
    .also { it.addAll(acceptedImageExt) }
private val acceptedReadAndEditExt = listOf(".pdf", ".md")

// todo: https://www.toptal.com/android/android-threading-all-you-need-to-know
//https://developer.android.com/reference/androidx/work/WorkManager.html
//https://developer.android.com/reference/androidx/core/app/JobIntentService.html

fun provideIconImage(file: Path): Path? =
    providePreview(file) //todo downscale to, say, 128x128

//might be a temporary file
fun providePreview(file: Path): Path? =
    if (isImage(file)) {
        file
    } else {
        if (false) { //todo: create previews from videos, pdfs, etc.
            TODO()
        } else {
            null
        }
    }

fun isImage(file: Path): Boolean {
    val name = file.fileName.toString()
    return acceptedImageExt.contains(name)
}

fun getFileActionType(filePath: Path): FileActionType{
    return when(".${extension(filePath)}"){
        in acceptedEditOnlyExt -> FileActionType.EDIT_AS_OPEN
        in acceptedReadAndEditExt -> FileActionType.EDIT_AND_OPEN
        else -> FileActionType.OPEN_ONLY
    }
}

fun isHidden(path: Path): Boolean =
    path.fileName.toString().startsWith('.')

fun folderExists(path: Path): Boolean =
    Files.exists(path) || Files.isDirectory(path)

fun isFileEmpty(filePath: Path?): Boolean =
    filePath == null || filePath.toString().trim().isEmpty()

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

fun reifySorting(sorting: Sorting): Comparator<Path>? =
    when (sorting) {
        Sorting.NAME -> compareBy { it.fileName }
        Sorting.SIZE -> compareBy { Files.size(it) }
        Sorting.TYPE -> compareBy { it.fileName.extension }
        Sorting.LAST_MODIFIED -> compareBy { Files.getLastModifiedTime(it) }
        Sorting.DEFAULT -> null
    }