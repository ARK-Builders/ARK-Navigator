package space.taran.arkbrowser.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.*
import java.nio.file.Files
import java.nio.file.Path

typealias Timestamp = Long
typealias StringPath = String

typealias MarkableFile = Pair<Boolean, Path>

//todo: java.io.File -> java.nio.Path

// todo: https://www.toptal.com/android/android-threading-all-you-need-to-know
//https://developer.android.com/reference/androidx/work/WorkManager.html
//https://developer.android.com/reference/androidx/core/app/JobIntentService.html

fun isImage(file: File): Boolean =
    when(file.extension) {
        "jpg", "jpeg", "png" -> true
        else -> false
    }

fun listChildren(folder: File): List<File> =
    folder.listFiles()?.let { files ->
        files.filter {
            !it.name.startsWith(".")
        }
    } ?: listOf()

fun listAllFiles(folder: File): List<File> {
    val (directories, files) = listChildren(folder)
        .partition { it.isDirectory }

    return files + directories.flatMap {
        listAllFiles(it)
    }
}

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

//todo: synchronize all usages
fun readBytes(file: File): ByteArray {
    return FileInputStream(file).readBytes()
}


fun getUriForFileByProvider(context: Context, file: File): Uri {
    return FileProvider.getUriForFile(context,
        "space.taran.arkbrowser.provider",
        file)
}

fun getExtSdCards(context: Context): List<Path> =
    context.getExternalFilesDirs("external")
        .toList()
        .filterNotNull()
        .map {
            it.toPath().toRealPath()
        }
//        .mapNotNull {
//            val path = it.absolutePath
//            todo: improve
//            val index = path.lastIndexOf("/Android/data")
//            if (index >= 0) {
//                File(path.substring(0, index))
//            } else {
//                null
//            }
//        }

fun getExtSdCardBaseFolder(context: Context, file: Path): Path? =
    getExtSdCards(context).find { file.startsWith(it) }
//todo fs.normalize `path` before check

fun extension(path: Path): String =
    path.fileName.toString()
        .split('.')
        .drop(1)
        .last()