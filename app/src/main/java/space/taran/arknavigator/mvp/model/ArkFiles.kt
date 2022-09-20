package space.taran.arknavigator.mvp.model

import space.taran.arknavigator.mvp.model.ArkFiles.ARK_FOLDER
import space.taran.arknavigator.mvp.model.ArkFiles.FAVORITES_FILE
import space.taran.arknavigator.mvp.model.ArkFiles.METADATA_FOLDER
import space.taran.arknavigator.mvp.model.ArkFiles.PREVIEWS_FOLDER
import space.taran.arknavigator.mvp.model.ArkFiles.STATS_FOLDER
import space.taran.arknavigator.mvp.model.ArkFiles.TAGS_STORAGE_FILE
import space.taran.arknavigator.mvp.model.ArkFiles.THUMBNAILS_FOLDER
import java.nio.file.Path

object ArkFiles {
    const val ARK_FOLDER = ".ark"
    const val STATS_FOLDER = "stats"
    const val FAVORITES_FILE = "favorites"
    const val TAGS_STORAGE_FILE = "tags"
    const val PREVIEWS_FOLDER = "previews"
    const val METADATA_FOLDER = "meta"
    const val THUMBNAILS_FOLDER = "thumbnails"
}

fun Path.arkFolder() = resolve(ARK_FOLDER)
fun Path.arkStats() = resolve(STATS_FOLDER)
fun Path.arkFavorites() = resolve(FAVORITES_FILE)
fun Path.arkTagsStorage() = resolve(TAGS_STORAGE_FILE)
fun Path.arkPreviews() = resolve(PREVIEWS_FOLDER)
fun Path.arkThumbnails() = resolve(THUMBNAILS_FOLDER)
fun Path.arkMetadata() = resolve(METADATA_FOLDER)
