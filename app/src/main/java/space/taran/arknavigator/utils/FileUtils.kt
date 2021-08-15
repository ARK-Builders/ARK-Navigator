package space.taran.arknavigator.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.*
import java.lang.IllegalArgumentException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

typealias Milliseconds = Long
typealias StringPath = String

typealias MarkableFile = Pair<Boolean, Path>

enum class Sorting {
    DEFAULT, NAME, SIZE, LAST_MODIFIED, TYPE
}

//todo: java.io.File -> java.nio.Path

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
    val result = name.endsWith(".jpg")
            || name.endsWith(".jpeg")
            || name.endsWith(".png")

    return result
}

fun isHidden(path: Path): Boolean =
    path.fileName.toString().startsWith('.')

fun folderExists(path: Path): Boolean =
    Files.exists(path) || Files.isDirectory(path)

//todo: synchronize all usages ?
fun checkOrCreate(file: File): Boolean {
    if (file.exists()) return true

    try {
        if (file.createNewFile())
            return true
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return false
}

//todo: synchronize all usages
fun remove(file: File): Boolean {
    if (!file.exists()) return true

    try {
        if (file.delete())
            return true
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return false
}

//todo: synchronize all usages
fun write(file: File, data: String) {
        val outputStream = FileOutputStream(file)
        outputStream.write(data.toByteArray())
        outputStream.close()
}

//todo: synchronize all usages
fun read(file: File): String {
    val inputStream = FileInputStream(file)
    val bufferedReader = BufferedReader(InputStreamReader(inputStream))
    var buffer: String?
    val stringBuilder = StringBuilder()
    buffer = bufferedReader.readLine()
    while (buffer != null) {
        stringBuilder.append(buffer).append("\n")
        buffer = bufferedReader.readLine()
    }

    inputStream.close()
    bufferedReader.close()

    return stringBuilder.toString()
}

//todo: synchronize all usages
fun readFirstLine(file: File): String {
    val inputStream = FileInputStream(file)
    val bufferedReader = BufferedReader(InputStreamReader(inputStream))
    val line = bufferedReader.readLine()

    inputStream.close()
    bufferedReader.close()

    return line
}

fun getUriForFileByProvider(context: Context, file: File): Uri {
    return FileProvider.getUriForFile(context,
        "space.taran.arknavigator.provider",
        file)
}

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

val ANDROID_DIRECTORY = Paths.get("Android")

val ROOT_PATH = Paths.get("/")

//fun getExtSdCardBaseFolder(context: Context, file: Path): Path? =
//    getExtSdCards(context).find { file.startsWith(it) }
//todo fs.normalize `path` before check

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