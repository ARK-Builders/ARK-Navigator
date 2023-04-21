package space.taran.arknavigator.utils

import space.taran.arknavigator.mvp.presenter.adapter.ResourceItem
import space.taran.arknavigator.ui.App
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.notExists

val ROOT_PATH: Path = Paths.get("/")

val ANDROID_DIRECTORY: Path = Paths.get("Android")

typealias Milliseconds = Long
typealias StringPath = String

enum class Sorting {
    DEFAULT, NAME, SIZE, LAST_MODIFIED, TYPE
}

fun convertIntToSorting(intValue: Int?): Sorting {
    return if (intValue == null) Sorting.DEFAULT
    else Sorting.values()[intValue]
}

fun listDevices(): List<Path> =
    App.instance
        .getExternalFilesDirs(null)
        .toList()
        .filterNotNull()
        .filter { it.exists() }
        .map {
            it.toPath().toRealPath()
                .takeWhile { part ->
                    part != ANDROID_DIRECTORY
                }
                .fold(ROOT_PATH) { parent, child ->
                    parent.resolve(child)
                }
        }

fun Path.findNotExistCopyName(name: Path): Path {
    val parentDir = this

    val originalNamePath = parentDir.resolve(name.fileName)
    if (originalNamePath.notExists())
        return originalNamePath

    var filesCounter = 1

    fun formatNameWithCounter() =
        "${name.nameWithoutExtension}_$filesCounter.${name.extension}"

    var newPath = parentDir.resolve(formatNameWithCounter())

    while (newPath.exists()) {
        newPath = parentDir.resolve(formatNameWithCounter())
        filesCounter++
    }
    return newPath
}

fun findLongestCommonPrefix(paths: List<Path>): Path {
    if (paths.isEmpty()) {
        throw IllegalArgumentException(
            "Can't search for common prefix among empty collection"
        )
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

fun reifySorting(sorting: Sorting): Comparator<ResourceItem>? =
    when (sorting) {
        Sorting.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.resource.name }
        Sorting.SIZE -> compareBy { it.resource.size() }
        Sorting.TYPE -> compareBy { it.resource.extension }
        Sorting.LAST_MODIFIED -> compareBy { it.resource.modified }
        Sorting.DEFAULT -> null
    }

const val KILOBYTE = 1024
const val MEGABYTE = 1024 * KILOBYTE
