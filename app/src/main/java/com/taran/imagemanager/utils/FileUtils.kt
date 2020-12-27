package space.taran.arkbrowser.utils

import space.taran.arkbrowser.mvp.model.entity.Folder

private val checkPaths = listOf("/storage/emulated/legacy", "/storage/emulated/0", "/mnt/sdcard")

fun checkInternalStorage(folder: Folder): Folder {
    checkPaths.find { path ->
        path == folder.path
    }?.let {
        folder.name = "Internal Storage"
    }

    return folder
}

fun isInternalStorage(folder: Folder): Boolean {
    checkPaths.forEach {
        if (folder.path.startsWith(it))
            return true
    }
    return false
}