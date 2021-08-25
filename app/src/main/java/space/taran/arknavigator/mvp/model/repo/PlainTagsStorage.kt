package space.taran.arknavigator.mvp.model.repo

import android.util.Log
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.utils.Constants.Companion.NO_TAGS
import space.taran.arknavigator.utils.Converters.Companion.stringFromTags
import space.taran.arknavigator.utils.Converters.Companion.tagsFromString
import space.taran.arknavigator.utils.TAGS_STORAGE
import space.taran.arknavigator.utils.Tags
import java.lang.IllegalStateException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime

// The storage is being read from the FS both during application startup
// and during application lifecycle since it can be changed from outside.
// We also must persist all changes during application lifecycle into FS.
class PlainTagsStorage
    private constructor(
        root: Path,
        resources: Collection<ResourceId>): TagsStorage {

    private val storageFile: Path = root.resolve(STORAGE_FILENAME)

    private var lastModified: FileTime = FileTime.fromMillis(0L)

    private val tagsById: MutableMap<ResourceId, Tags> = {
        val result = resources.map { it to NO_TAGS }
            .toMap()
            .toMutableMap()

        if (Files.exists(storageFile)) {
            lastModified = Files.getLastModifiedTime(storageFile)

            Log.d(
                TAGS_STORAGE, "file $storageFile exists" +
                        ", last modified at $lastModified"
            )

            result.putAll(readStorage())
        } else {
            Log.d(TAGS_STORAGE, "file $storageFile doesn't exist")
        }

        result
    }()

    override fun contains(id: ResourceId): Boolean = tagsById.containsKey(id)

    // if this id isn't present in storage, then the call is wrong
    // because the caller always takes this id from ResourcesIndex
    // and the index and storage must be in sync
    override fun getTags(id: ResourceId): Tags = tagsById[id]!!
    //todo: check the file's modification date and pull external updates

    override fun setTags(id: ResourceId, tags: Tags) {
        if (!tagsById.containsKey(id)) {
            throw AssertionError("Storage isn't aware about this resource id")
        }

        Log.d(TAGS_STORAGE, "new tags for resource $id: $tags")
        tagsById[id] = tags

        persist()
    }

    override fun listUntaggedResources(): Set<ResourceId> =
        tagsById
            .filter { (_, tags) -> tags.isEmpty() }
            .keys

    override fun cleanup(existing: Collection<ResourceId>) {
        val disappeared = tagsById.keys.minus(existing)

        Log.d(TAGS_STORAGE, "forgetting ${disappeared.size} resources")
        disappeared.forEach { tagsById.remove(it) }
        persist()
    }

    override fun remove(id: ResourceId) {
        Log.d(TAGS_STORAGE, "forgetting resource $id")
        tagsById.remove(id)
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

        if (tagsById.isEmpty() || tagsById.all { it.value.isEmpty() }) {
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

                if (tags.isEmpty()) {
                    throw AssertionError("Tags storage must not contain empty sets of tags")
                }

                id to tags
            }
            .toMap()

        if (result.isEmpty()) {
            throw AssertionError("Tags storage must not be empty")
        }

        Log.d(TAGS_STORAGE, "${result.size} entries has been read")
        return result
    }

    private fun writeStorage() {
        val lines = mutableListOf<String>()
        lines.add("$STORAGE_VERSION_PREFIX$STORAGE_VERSION")

        val entries = tagsById.filterValues { it.isNotEmpty() }
        lines.addAll(entries.map { (id, tags) ->
            "$id$KEY_VALUE_SEPARATOR ${stringFromTags(tags)}"
        })

        Files.write(storageFile, lines, StandardCharsets.UTF_8)
        lastModified = Files.getLastModifiedTime(storageFile)

        Log.d(TAGS_STORAGE, "${tagsById.size} entries has been written")
    }

    //todo: clean up storage when items are removed
    // (OR their ids are present but files not found)
    // (+ trash can for tags of removed resources)

    companion object {
        const val STORAGE_FILENAME = ".ark-tags"

        private const val STORAGE_VERSION = 2
        private const val STORAGE_VERSION_PREFIX = "version "

        const val KEY_VALUE_SEPARATOR = ':'

        private val storageByRoot = mutableMapOf<Path, PlainTagsStorage>()

        fun provide(root: Path, resources: Collection<ResourceId>): PlainTagsStorage {
            val storage = storageByRoot[root]
            if (storage != null) {
                if (storage.tagsById.keys.toSet() != resources.toSet()) {
                    Log.d(TAGS_STORAGE, "resources in the storage: ${storage.tagsById.keys}")
                    Log.d(TAGS_STORAGE, "resources in the index: $resources")
                    throw AssertionError("Index and storage diverged")
                }
                return storage
            }

            val fresh = PlainTagsStorage(root, resources)
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