package com.taran.imagemanager.ui.file

import android.content.Context
import com.taran.imagemanager.mvp.model.file.FileProvider

class AndroidFileProvider(val context: Context): FileProvider {
    override fun getStorages(): List<String> {
        val files = context.getExternalFilesDirs(null)
        val base = "/Android/data/${context.packageName}/files"

        return files
            .map { file ->
                file.absolutePath.replace(base, "")
            }
    }
}