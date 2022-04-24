package space.taran.arknavigator.mvp.model

import space.taran.arknavigator.mvp.model.ArkFiles.ARK_FOLDER
import space.taran.arknavigator.mvp.model.ArkFiles.FAVORITES_FILE
import space.taran.arknavigator.mvp.model.ArkFiles.TAGS_STORAGE_FILE
import java.nio.file.Path
import kotlin.io.path.createDirectories

object ArkFiles {
    const val ARK_FOLDER = ".ark"
    const val FAVORITES_FILE = "favorites"
    const val TAGS_STORAGE_FILE = "tags"
}

fun Path.arkFolder() = resolve(ARK_FOLDER).createDirectories()
fun Path.arkFavorites() = resolve(FAVORITES_FILE)
fun Path.arkTagsStorage() = resolve(TAGS_STORAGE_FILE)
