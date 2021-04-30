package space.taran.arkbrowser.mvp.model.repo

import space.taran.arkbrowser.mvp.model.entity.room.ResourceId
import space.taran.arkbrowser.utils.Converters.Companion.tagsFromString
import space.taran.arkbrowser.utils.Tags
import java.lang.IllegalStateException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime

// The storage is being read from the FS both during application startup
// and during application lifecycle since it can be changed from outside.
// We also must persist all changes during application lifecycle into FS.
class TagsStorage private constructor(private val root: Path) {
    private val storageFile = root.resolve(STORAGE_FILENAME)

    private var lastModified: FileTime = FileTime.fromMillis(0L)

    private val tagsById: MutableMap<ResourceId, Tags> =
        if (Files.exists(storageFile)) {
            lastModified = Files.getLastModifiedTime(storageFile)

            val lines = Files.readAllLines(storageFile, StandardCharsets.UTF_8)
            verifyVersion(lines.removeAt(0))

            lines.map {
                    val parts = it.split(KEY_VALUE_SEPARATOR)
                    parts[0].toLong() to tagsFromString(parts[1])
                }
                .toMap()
                .toMutableMap()
        } else {
            mutableMapOf()
        }

    //todo tags query functions, with checking lastModified

    //todo tags modification, with immediate writing to the storageFile, with checking lastModified

    //todo background listening to changes in FileSystem

    companion object {
        const val STORAGE_FILENAME = ".ark-tags"

        const val STORAGE_VERSION = 2
        const val STORAGE_VERSION_PREFIX = "version "

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