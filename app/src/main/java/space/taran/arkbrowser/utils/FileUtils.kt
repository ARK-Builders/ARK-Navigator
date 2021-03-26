package space.taran.arkbrowser.utils

import space.taran.arkbrowser.mvp.model.entity.File

private val internalPaths = listOf("/storage/emulated/legacy", "/storage/emulated/0", "/mnt/sdcard")

fun isInternalStorage(file: File): Boolean {
    internalPaths.forEach {
        if (file.path == it)
            return true
    }
    return false
}