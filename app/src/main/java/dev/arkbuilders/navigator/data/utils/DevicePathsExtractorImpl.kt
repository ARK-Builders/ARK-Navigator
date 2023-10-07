package dev.arkbuilders.navigator.data.utils

import dev.arkbuilders.navigator.presentation.App
import java.nio.file.Path
import javax.inject.Inject


class FileUtilsImpl @Inject constructor(
    private val appInstance: App
) : FileUtils {

    override fun listDevices(): List<Path> =
        appInstance
            .getExternalFilesDirs(null)
            .toList()
            .filterNotNull()
            .filter { it.exists() }
            .map {
                it.toPath().toRealPath()
                    .takeWhile { part ->
                        part != ANDROID_DIRECTORY
                    }
                    .fold(ROOT_PATH) { parent, child ->
                        parent.resolve(child)
                    }
            }
}
