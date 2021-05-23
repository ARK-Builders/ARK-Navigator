package space.taran.arkbrowser.mvp.model.repo

import android.util.Log
import space.taran.arkbrowser.mvp.model.dao.ResourceId
import space.taran.arkbrowser.utils.Converters.Companion.stringFromTags
import space.taran.arkbrowser.utils.Converters.Companion.tagsFromString
import space.taran.arkbrowser.utils.TAGS_STORAGE
import space.taran.arkbrowser.utils.Tags
import java.lang.IllegalStateException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime

//todo
//        val storage = resourcesRepo.createStorage(pickedDir!!)
//        if (storage == null) {
//            requestSdCardUri()
//            return
//        }

// The storage is being read from the FS both during application startup
// and during application lifecycle since it can be changed from outside.
// We also must persist all changes during application lifecycle into FS.
class PlainTagsStorage private constructor(root: Path): TagsStorage {
    private val storageFile: Path = root.resolve(STORAGE_FILENAME)

    private var lastModified: FileTime = FileTime.fromMillis(0L)

    private val tagsById: MutableMap<ResourceId, Tags> =
        if (Files.exists(storageFile)) {
            lastModified = Files.getLastModifiedTime(storageFile)

            Log.d(TAGS_STORAGE, "file $storageFile exists" +
                ", last modified at $lastModified")

            readStorage().toMutableMap()
        } else {
            Log.d(TAGS_STORAGE, "file $storageFile doesn't exist")
            mutableMapOf()
        }

    //todo `listAllTags`

    override fun listTags(id: ResourceId): Tags = tagsById[id] ?: setOf()

    override fun listResources(): Set<ResourceId> = tagsById.keys

    override fun forgetResources(ids: Collection<ResourceId>) {
        Log.d(TAGS_STORAGE, "forgetting ${ids.size} resources")
        ids.forEach { tagsById.remove(it) }
        persist()
    }

    private fun persist() {
        val exists = Files.exists(storageFile)
        val modified = if (exists) {
            Files.getLastModifiedTime(storageFile)
        } else {
            //storage file could be deleted from outside and, right now,
            //we deal with this case by just re-creating the file,
            //because merging of removals is not implemented yet
            FileTime.fromMillis(0)
            //that also means that we can't drop storage on other device
            //while we have the app running on another one
            //(assume external files synchronization)
        }

        if (modified > lastModified) {
            Log.d(TAGS_STORAGE, "storage file was modified externally, merging")
            //todo: for real merge we need to track our own changes locally
            //without this, we need to stop all competing devices to remove a tag or a resource
            //so far, just ensuring that we are not losing additions

            val outside = readStorage()

            for (newId in outside.keys - tagsById.keys) {
                Log.d(TAGS_STORAGE, "resource $newId got first tags from outside")
                tagsById[newId] = outside[newId]!!
            }

            for (sharedId in outside.keys.intersect(tagsById.keys)) {
                val theirs = outside[sharedId]
                val ours = tagsById[sharedId]
                if (theirs != ours) {
                    Log.d(TAGS_STORAGE, "resource $sharedId got new tags " +
                        "from outside: ${theirs!! - ours}")
                    tagsById[sharedId] = theirs.union(ours!!)
                }
            }
        }

        if (tagsById.isEmpty()) {
            if (exists) {
                Log.d(TAGS_STORAGE, "no tagged resources, deleting storage file")
                Files.delete(storageFile)
            }
            return
        }

        writeStorage()
    }

    private fun readStorage(): Map<ResourceId, Tags> {
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

        if (result.isEmpty()) throw AssertionError(
            "Tags storage must not be empty")

        Log.d(TAGS_STORAGE, "${result.size} entries has been read")
        return result
    }

    private fun writeStorage() {
        val lines = mutableListOf<String>()

        lines.add("$STORAGE_VERSION_PREFIX$STORAGE_VERSION")
        lines.addAll(tagsById.map { (id, tags) ->
            "$id$KEY_VALUE_SEPARATOR ${stringFromTags(tags)}"
        })

        Files.write(storageFile, lines, StandardCharsets.UTF_8)
        lastModified = Files.getLastModifiedTime(storageFile)

        Log.d(TAGS_STORAGE, "${tagsById.size} entries has been written")
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

        private val storageByRoot = mutableMapOf<Path, PlainTagsStorage>()

        fun provide(root: Path): PlainTagsStorage {
            val storage = storageByRoot[root]
            if (storage != null) {
                return storage
            }

            val fresh = PlainTagsStorage(root)
            storageByRoot[root] = fresh
            return fresh
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