package space.taran.arknavigator.utils

import android.content.Context
import java.lang.IllegalArgumentException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

val ROOT_PATH: Path = Paths.get("/")

val ANDROID_DIRECTORY: Path = Paths.get("Android")

typealias Milliseconds = Long
typealias StringPath = String

enum class Sorting {
    DEFAULT, NAME, SIZE, LAST_MODIFIED, TYPE
}

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
    val result = name.endsWith(".jpg")
            || name.endsWith(".jpeg")
            || name.endsWith(".png")

    return result
}

fun isHidden(path: Path): Boolean =
    path.fileName.toString().startsWith('.')

fun folderExists(path: Path): Boolean =
    Files.exists(path) || Files.isDirectory(path)

fun listDevices(context: Context): List<Path> =
    context.getExternalFilesDirs(null)
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

fun extension(path: Path): String =
    path.fileName.toString()
        .split('.')
        .drop(1)
        .last()