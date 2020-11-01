package com.taran.imagemanager.ui.file

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.taran.imagemanager.mvp.model.entity.Image
import com.taran.imagemanager.mvp.model.file.FileProvider


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
}