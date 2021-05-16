package space.taran.arkbrowser.mvp.model.repo

import android.util.Log
import space.taran.arkbrowser.mvp.model.entity.room.ResourceId
import space.taran.arkbrowser.utils.Converters.Companion.tagsFromString
import space.taran.arkbrowser.utils.TAGS_STORAGE
import space.taran.arkbrowser.utils.Tags
import java.lang.IllegalStateException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime

//todo
//                list.forEach { root ->
//                    val storageVersion = resourcesRepo.readStorageVersion(root.storage)
//                    if (storageVersion != ResourcesRepo.STORAGE_VERSION)
//                        storageVersionDifferent(storageVersion, root)
//                    rootsRepo.synchronizeRoot(root)
//                }

//todo
//        val storage = resourcesRepo.createStorage(pickedDir!!)
//        if (storage == null) {
//            requestSdCardUri()
//            return
//        }

// The storage is being read from the FS both during application startup
// and during application lifecycle since it can be changed from outside.
// We also must persist all changes during application lifecycle into FS.
class TagsStorage private constructor(root: Path) {
    private val storageFile = root.resolve(STORAGE_FILENAME)

    private var lastModified: FileTime = FileTime.fromMillis(0L)

    private val tagsById: MutableMap<ResourceId, Tags> =
        if (Files.exists(storageFile)) {
            lastModified = Files.getLastModifiedTime(storageFile)

            Log.d(TAGS_STORAGE, "file $storageFile exists" +
                ", last modified at $lastModified")

            val lines = Files.readAllLines(storageFile, StandardCharsets.UTF_8)
            verifyVersion(lines.removeAt(0))

            val result = lines
                .map {
                    val parts = it.split(KEY_VALUE_SEPARATOR)
                    val id = parts[0].toLong()
                    val tags = tagsFromString(parts[1])

                    if (tags.isEmpty()) throw AssertionError(
                        "Tags storage must not contain empty sets of tags")

                    id to tags
                }
                .toMap()
                .toMutableMap()

            if (result.isEmpty()) throw AssertionError(
                "Tags storage must not be empty")

            Log.d(TAGS_STORAGE, result.toString())
            result
        } else {
            Log.d(TAGS_STORAGE, "file $storageFile doesn't exist")
            mutableMapOf()
        }

    //todo tags query functions, with checking lastModified

    //todo tags modification, with immediate writing to the storageFile, with checking lastModified

    //todo background listening to changes in FileSystem

    //todo: clean up storage when items are removed
    // (OR their ids are present but files not found)

    companion object {
        const val STORAGE_FILENAME = ".ark-tags"

        private const val STORAGE_VERSION = 2
        private const val STORAGE_VERSION_PREFIX = "version "

        const val KEY_VALUE_SEPARATOR = ':'

        private val boundPaths = mutableSetOf<Path>()

        fun provide(root: Path): TagsStorage =
            if (boundPaths.contains(root)) {
                throw AssertionError("The root $root is already bound")
            } else {
                boundPaths.add(root)
                TagsStorage(root)
            }

        private fun verifyVersion(header: String) {
            if (!header.startsWith(STORAGE_VERSION_PREFIX)) {
                throw IllegalStateException("Unknown storage version")
            }
            val version = header.removePrefix(STORAGE_VERSION_PREFIX).toInt()

            if (version > STORAGE_VERSION) {
                throw IllegalStateException("Storage format is newer than the app")
            }
            if (version < STORAGE_VERSION) {
                throw IllegalStateException("Storage format is older than the app")
            }
        }
    }
}