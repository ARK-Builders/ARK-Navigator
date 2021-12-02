package space.taran.arknavigator.utils

import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.ui.App
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.listDirectoryEntries

val ROOT_PATH: Path = Paths.get("/")

val ANDROID_DIRECTORY: Path = Paths.get("Android")

typealias Milliseconds = Long
typealias StringPath = String

enum class Sorting {
    DEFAULT, NAME, SIZE, LAST_MODIFIED, TYPE
}

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

fun listChildren(folder: Path): Pair<List<Path>, List<Path>> = folder
    .listDirectoryEntries()
    .filter { !Files.isHidden(it) }
    .partition { Files.isDirectory(it) }

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

fun extension(path: Path): String {
    return path.extension.lowercase()
}

fun reifySorting(sorting: Sorting): Comparator<ResourceMeta>? =
    when (sorting) {
        Sorting.NAME -> compareBy { it.name }
        Sorting.SIZE -> compareBy { it.size }
        Sorting.TYPE -> compareBy { it.extension }
        Sorting.LAST_MODIFIED -> compareBy { it.modified }
        Sorting.DEFAULT -> null
    }

const val KILOBYTE = 1024
const val MEGABYTE = 1024 * KILOBYTE