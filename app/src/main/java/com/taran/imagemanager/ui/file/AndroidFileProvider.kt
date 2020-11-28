package com.taran.imagemanager.ui.file

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.taran.imagemanager.mvp.model.entity.Folder
import com.taran.imagemanager.mvp.model.entity.Image
import com.taran.imagemanager.mvp.model.file.FileProvider
import java.io.File
import java.io.IOException


class AndroidFileProvider(val context: Context): FileProvider {

    override fun getExternalStorage(): String {
        return Environment.getExternalStorageDirectory().absolutePath
    }

    override fun getImagesFromGallery(): List<Image> {
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
        var imageName: String
        val images = mutableListOf<Image>()

        cursor?.let {
            val columnIndexPath = cursor.getColumnIndexOrThrow(projection[0])
            val columnIndexName = cursor.getColumnIndexOrThrow(projection[1])
            if (it.moveToFirst()) {
                while (cursor.moveToNext()) {
                    imagePath = cursor.getString(columnIndexPath)
                    imageName = cursor.getString(columnIndexName)
                    images.add(Image(name = imageName, path = imagePath))
                }
            }

            it.close()
        }

        return images
    }

    override fun getExtSdCards(): List<Folder> {
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