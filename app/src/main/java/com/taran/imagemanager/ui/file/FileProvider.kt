package com.taran.imagemanager.ui.file

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.taran.imagemanager.mvp.model.entity.Folder
import com.taran.imagemanager.mvp.model.entity.Image
import com.taran.imagemanager.mvp.model.entity.room.CardUri
import com.taran.imagemanager.utils.TEXT_STORAGE_NAME
import java.io.*


class FileProvider(val context: Context) {
    val TEXT_MIME_TYPE = "text/plain"
    var cardUris = mutableListOf<CardUri>()
    val DUMMY_FILE_NAME = "dummy.txt"

    fun getExternalStorage(): String {
        return Environment.getExternalStorageDirectory().absolutePath
    }

    fun getImagesFromGallery(): List<Image> {
        val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )

        val cursor = context.contentResolver.query(
            contentUri,
            projection,
            null,
            null,
            MediaStore.Images.Media.DATE_ADDED + " DESC"
        )

        var imagePath: String
        var imageName: String?
        val images = mutableListOf<Image>()

        cursor?.let {
            val columnIndexPath = cursor.getColumnIndexOrThrow(projection[0])
            val columnIndexName = cursor.getColumnIndexOrThrow(projection[1])
            if (it.moveToFirst()) {
                while (cursor.moveToNext()) {
                    imagePath = cursor.getString(columnIndexPath)
                    imageName = cursor.getString(columnIndexName)
                    if (imageName == null) {
                        imageName = "Unknown"
                        Log.i("UnknownImageName", imagePath)
                    }
                    images.add(Image(name = imageName!!, path = imagePath))
                }
            }

            it.close()
        }


        return images
    }

    fun canWrite(filePath: String): Boolean {
        val file = File(filePath)
        val dummyPath = "${file.parent}/$DUMMY_FILE_NAME"
        return if (mkFile(dummyPath)) {
            try {
                writeToFile(dummyPath, "test")
                removeFile(dummyPath)
                true
            } catch (e: Exception) {
                false
            }
        } else
            false
    }

    fun mkFile(filePath: String): Boolean {
        val file = File(filePath)

        if (file.exists()) return true

        try {
            if (file.createNewFile())
                return true
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val parentFolder = getDocumentFile(file.parent!!)

            return parentFolder.createFile(TEXT_MIME_TYPE, file.name) != null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    fun removeFile(filePath: String): Boolean {
        val file = File(filePath)

        if (!file.exists()) return true

        try {
            if (file.delete())
                return true
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val document = getDocumentFile(filePath)
            return document.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    private fun getDocumentFile(path: String): DocumentFile {

        val baseFolder = getExtSdCardsBaseFolder(path)
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

    fun writeToFile(path: String, data: String): Boolean {
        val outputStream = getFileOutputStream(path)
        outputStream.write(data.toByteArray())
        outputStream.close()
        return true
    }

    fun readFromFile(path: String): String {
        val inputStream = getFileInputStream(path)
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

    fun getFileInputStream(path: String): InputStream {
        var inputStream: InputStream
        try {
            inputStream = FileInputStream(path)
            return inputStream
        } catch (e: Exception) {
        }

        val document = getDocumentFile(path)
        inputStream = context.contentResolver.openInputStream(document.uri)!!
        return inputStream
    }

    fun getFileOutputStream(path: String): OutputStream {
        var outputStream: OutputStream
        try {
            outputStream = FileOutputStream(path)
            return outputStream
        } catch (e: Exception) {
        }

        val document = getDocumentFile(path)
        outputStream = context.contentResolver.openOutputStream(document.uri)!!
        return outputStream
    }

    fun getUriByPath(filePath: String): Uri? {
        val base = getExtSdCardsBaseFolder(filePath)
        cardUris.forEach {
            if (it.path == base)
                return Uri.parse(it.uri)
        }
        return null
    }

    fun getExtSdCardsBaseFolder(filePath: String): String? {
        val sdPaths = getExtSdCards().map { it.path }
        sdPaths.forEach { sdPath ->
            if (filePath.startsWith(sdPath))
                return sdPath
        }

        return null
    }

    fun isBaseFolder(path: String): Boolean {
        val file = File(path)
        val sdPaths = getExtSdCards().map { it.path }
        sdPaths.forEach { sdPath ->
            if (sdPath == file.parent)
                return true
        }
        return false
    }

    fun getExtSdCards(): List<Folder> {
        val folders: MutableList<Folder> = ArrayList()
        for (file in context.getExternalFilesDirs("external")) {
            if (file != null) {
                val index = file.absolutePath.lastIndexOf("/Android/data")
                if (index >= 0) {
                    val path = file.absolutePath.substring(0, index)
                    try {
                        val folder = File(path)
                        folders.add(Folder(name = folder.name, path = folder.canonicalPath))
                    } catch (e: IOException) {
                        // Keep non-canonical path.
                    }
                }
            }
        }
        //if (folders.isEmpty()) folders.add("/storage/sdcard1")
        return folders
    }

}