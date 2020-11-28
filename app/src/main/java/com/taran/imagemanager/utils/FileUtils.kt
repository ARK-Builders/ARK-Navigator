package com.taran.imagemanager.utils

import com.taran.imagemanager.mvp.model.entity.Folder

fun checkInternalStorage(folder: Folder): Folder {
    val checkPaths = listOf("/storage/emulated/legacy", "/storage/emulated/0", "/mnt/sdcard")
    checkPaths.find { path ->
        path == folder.path
    }?.let {
        folder.name = "Internal Storage"
    }

    return folder
}