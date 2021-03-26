package space.taran.arkbrowser.ui.file

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import space.taran.arkbrowser.mvp.model.entity.room.CardUri
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class DocumentProvider(val context: Context, val fileProvider: FileProvider) {

    var cardUris = mutableListOf<CardUri>()

    fun getFileUri(path: String): String {
        val document = DocumentFile.fromFile(File(path))
        return document.uri.toString()
    }

    fun getMimeType(path: String): String {
        val document = DocumentFile.fromFile(File(path))
        return document.type!!
    }

    fun mk(parentPath: String, name: String, mimeType: String): Boolean {
        return try {
            val parentFolder = getDocumentFile(parentPath)
            parentFolder.createFile(mimeType, name)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun remove(path: String): Boolean {
        val document = getDocumentFile(path)
        return document.delete()
    }

    fun write(path: String, data: String): Boolean {
        synchronized(this) {
            val outputStream =
                context.contentResolver.openOutputStream(getDocumentFile(path).uri)
            outputStream!!.write(data.toByteArray())
            outputStream.close()
            return true
        }
    }

    fun read(path: String): String {
        synchronized(this) {
            val inputStream = context.contentResolver.openInputStream(getDocumentFile(path).uri)
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            var buffer: String?
            val stringBuilder = StringBuilder()
            buffer = bufferedReader.readLine()
            while (buffer != null) {
                stringBuilder.append(buffer).append("\n")
                buffer = bufferedReader.readLine()
            }

            inputStream?.close()
            bufferedReader.close()

            return stringBuilder.toString()
        }
    }

    fun getBytes(path: String): ByteArray {
        synchronized(this) {
            return context.contentResolver.openInputStream(getDocumentFile(path).uri)!!.readBytes()
        }
    }

    private fun getDocumentFile(path: String): DocumentFile {
        val baseFolder = fileProvider.getExtSdCardBaseFolder(path)
        var sameFolder = false
        var relativePath: String? = null

        if (!baseFolder.equals(path)) relativePath = path.substring(baseFolder!!.length + 1)
        else sameFolder = true

        val treeUri = getUriByPath(path)!!
        var document = DocumentFile.fromTreeUri(context, treeUri)!!
        if (sameFolder) return document

        val parts = relativePath!!.split("/")

        parts.forEach { part ->
            document = document.findFile(part)!!
        }

        return document
    }

    private fun getUriByPath(path: String): Uri? {
        val base = fileProvider.getExtSdCardBaseFolder(path)
        cardUris.forEach {
            if (it.path == base)
                return Uri.parse(it.uri)
        }
        return null
    }
}