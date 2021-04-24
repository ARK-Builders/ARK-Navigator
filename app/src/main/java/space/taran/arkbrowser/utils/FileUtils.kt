package space.taran.arkbrowser.utils

import android.util.Log
import java.io.*

typealias Timestamp = Long
typealias StringPath = String

typealias MarkableFile = Pair<Boolean, File>

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
fun write(file: File, data: String): Boolean {
    return try {
        val outputStream = FileOutputStream(file)
        outputStream.write(data.toByteArray())
        outputStream.close()
        true
    } catch (e: Exception) {
        false
    }
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