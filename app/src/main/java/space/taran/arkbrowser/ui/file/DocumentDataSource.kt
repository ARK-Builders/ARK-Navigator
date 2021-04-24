package space.taran.arkbrowser.ui.file

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import space.taran.arkbrowser.mvp.model.entity.room.SDCardUri
import java.io.File
import java.lang.RuntimeException
import java.nio.file.Path

class DocumentDataSource(val context: Context, private val fileDataSource: FileDataSource) {

    var sdCardUris = mutableListOf<SDCardUri>()

    fun createDocument(parent: File, name: String, mimeType: String): Boolean {
        return try {
            val parentFolder = getDocumentFile(parent)
            parentFolder.createFile(mimeType, name)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun remove(file: File): Boolean {
        val document = getDocumentFile(file)
        return document.delete()
    }

    fun write(file: File, data: String): Boolean {
        synchronized(this) {
            val outputStream =
                context.contentResolver.openOutputStream(getDocumentFile(file).uri)
            outputStream!!.write(data.toByteArray())
            outputStream.close()
            return true
        }
    }

//    fun read(file: File): String {
//        synchronized(this) {
//            val inputStream = context.contentResolver.openInputStream(getDocumentFile(file).uri)
//            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
//            var buffer: String?
//            val stringBuilder = StringBuilder()
//            buffer = bufferedReader.readLine()
//            while (buffer != null) {
//                stringBuilder.append(buffer).append("\n")
//                buffer = bufferedReader.readLine()
//            }
//
//            inputStream?.close()
//            bufferedReader.close()
//
//            return stringBuilder.toString()
//        }
//    }

    private fun getDocumentFile(file: File): DocumentFile {
        val baseFolder = fileDataSource.getExtSdCardBaseFolder(file)
        val relativePath: Path = file.relativeTo(baseFolder!!).toPath()

        val treeUri = getUriByFile(file)!!
        var document = DocumentFile.fromTreeUri(context, treeUri)!!

        val partsCount = relativePath.nameCount
        for (i in 0 until partsCount) {
            val name = relativePath.getName(i).toString()
            document = document.findFile(name)
                ?: throw RuntimeException(
                    "Couldn't find a file with name $name in $document")
        }

        return document
    }

    private fun getUriByFile(file: File): Uri? {
        val base = fileDataSource.getExtSdCardBaseFolder(file)

        val sdCardUri = sdCardUris.find { it.toString() == base.toString() }
        //todo: compare URIs directly

        return Uri.parse(sdCardUri?.uri)
    }
}
