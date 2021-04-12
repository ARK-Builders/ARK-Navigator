package space.taran.arkbrowser.ui.file

import android.content.Context
import androidx.core.content.FileProvider
import space.taran.arkbrowser.mvp.model.entity.File
import java.io.*

class FileDataSource(val context: Context) {

    fun getFile(path: String): File {
        val file = java.io.File(path)
        return mapToFile(file)
    }

    fun getUriForFileByProvider(path: String): String {
        val file = java.io.File(path)
        return FileProvider.getUriForFile(context, "space.taran.arkbrowser.provider", file).toString()
    }

    fun getParent(path: String): File? {
        val file = java.io.File(path)
        val parent = file.parentFile
        parent?.let {
            return mapToFile(it)
        } ?: let {
            return null
        }
    }

    fun mk(filePath: String): Boolean {
        val file = java.io.File(filePath)

        if (file.exists()) return true

        try {
            if (file.createNewFile())
                return true
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    fun remove(filePath: String): Boolean {
        val file = java.io.File(filePath)

        if (!file.exists()) return true

        try {
            if (file.delete())
                return true
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    fun write(path: String, data: String): Boolean {
        synchronized(this) {
            return try {
                val outputStream = FileOutputStream(path)
                outputStream.write(data.toByteArray())
                outputStream.close()
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    fun read(path: String): String {
        synchronized(this) {
            val inputStream = FileInputStream(path)
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
    }

    fun readFirstLine(path: String): String {
        synchronized(this) {
            val inputStream = FileInputStream(path)
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            val line = bufferedReader.readLine()

            inputStream.close()
            bufferedReader.close()

            return line
        }
    }

    fun list(path: String): List<File> {
        val parent = java.io.File(path)
        return parent.listFiles()?.let { files ->
            files.map {
                mapToFile(it)
            }.filter {
                !it.name.startsWith(".")
            }
        } ?: listOf()
    }

    fun getBytes(path: String): ByteArray {
        return FileInputStream(path).readBytes()
    }

    fun getLastModified(path: String): Long {
        return File(path).lastModified()
    }

    fun getExtSdCards(): List<File> {
        val folders: MutableList<File> = ArrayList()
        for (file in context.getExternalFilesDirs("external")) {
            if (file != null) {
                val index = file.absolutePath.lastIndexOf("/Android/data")
                if (index >= 0) {
                    val path = file.absolutePath.substring(0, index)
                    try {
                        val folder = java.io.File(path)
                        folders.add(mapToFile(folder))
                    } catch (e: IOException) {
                        // Keep non-canonical path.
                    }
                }
            }
        }
        //if (folders.isEmpty()) folders.add("/storage/sdcard1")
        return folders
    }

    fun getExtSdCardBaseFolder(path: String): String? {
        val sdPaths = getExtSdCards().map { it.path }
        sdPaths.forEach { sdPath ->
            if (path.startsWith(sdPath))
                return sdPath
        }

        return null
    }

    private fun mapToFile(file: java.io.File): File {
        return File(
            name = file.name,
            type = file.extension,
            path = file.absolutePath,
            size = file.length(),
            lastModified = file.lastModified(),
            isFolder = file.isDirectory
        )
    }
}