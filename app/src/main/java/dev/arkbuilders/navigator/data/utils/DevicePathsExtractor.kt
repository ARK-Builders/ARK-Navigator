package dev.arkbuilders.navigator.data.utils

import java.nio.file.Path


enum class Sorting {
    DEFAULT, NAME, SIZE, LAST_MODIFIED, TYPE
}
interface FileUtils {
    fun listDevices(): List<Path>

}
