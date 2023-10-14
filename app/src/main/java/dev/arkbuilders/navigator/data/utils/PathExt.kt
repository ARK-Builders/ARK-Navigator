package dev.arkbuilders.navigator.data.utils

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.notExists

val ROOT_PATH: Path = Paths.get("/")

val ANDROID_DIRECTORY: Path = Paths.get("Android")

fun Path.findNotExistCopyName(name: Path): Path {
    val originalNamePath = this.resolve(name.fileName)
    if (originalNamePath.notExists())
        return originalNamePath

    var filesCounter = 1

    val formatNameWithCounter =
        "${name.nameWithoutExtension}_$filesCounter.${name.extension}"

    var newPath = this.resolve(formatNameWithCounter)

    while (newPath.exists()) {
        newPath = this.resolve(formatNameWithCounter)
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

    return tailrec(ROOT_PATH, paths).first
}

private fun tailrec(prefix: Path, paths: List<Path>): Pair<Path, List<Path>> {
    val grouped = paths.groupBy { it.getName(0) }
    if (grouped.size > 1) {
        return prefix to paths
    }

    val resolvedPrefix = prefix.resolve(grouped.keys.first())
    val shortened = grouped.values.first()
        .map { resolvedPrefix.relativize(it) }

    return tailrec(resolvedPrefix, shortened)
}
