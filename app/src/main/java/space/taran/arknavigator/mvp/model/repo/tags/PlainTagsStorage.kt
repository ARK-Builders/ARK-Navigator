package space.taran.arknavigator.mvp.model.repo.tags

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.taran.arknavigator.mvp.model.arkFolder
import space.taran.arknavigator.mvp.model.arkTagsStorage
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.preferences.PreferenceKey
import space.taran.arknavigator.mvp.model.repo.preferences.Preferences
import space.taran.arknavigator.mvp.model.repo.stats.StatsEvent
import space.taran.arknavigator.utils.Constants.Companion.NO_TAGS
import space.taran.arknavigator.utils.Converters.Companion.stringFromTags
import space.taran.arknavigator.utils.Converters.Companion.tagsFromString
import space.taran.arknavigator.utils.LogTags.TAGS_STORAGE
import space.taran.arknavigator.utils.Tags
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.notExists

// The storage is being read from the FS both during application startup
// and during application lifecycle since it can be changed from outside.
// We also must persist all changes during application lifecycle into FS.
class PlainTagsStorage(
    val root: Path,
    val resources: Collection<ResourceId>,
    private val statsFlow: MutableSharedFlow<StatsEvent>,
    private val scope: CoroutineScope,
    private val preferences: Preferences
) : TagsStorage {

    private val storageFile: Path = root.arkFolder().arkTagsStorage()

    private var lastModified: FileTime = FileTime.fromMillis(0L)

    private lateinit var tagsById: MutableMap<ResourceId, Tags>

    suspend fun init() = withContext(Dispatchers.IO) {
        val result = resources.map { it to NO_TAGS }
            .toMap()
            .toMutableMap()
        if (Files.exists(storageFile)) {
            lastModified = Files.getLastModifiedTime(storageFile)

            Log.d(
                TAGS_STORAGE,
                "file $storageFile exists" +
                    ", last modified at $lastModified"
            )

            result.putAll(readStorage())
        } else {
            Log.d(TAGS_STORAGE, "file $storageFile doesn't exist")
        }
        tagsById = result
    }

    fun register(id: ResourceId) {
        // this registration isn't really needed,
        // but should help to prevent bugs, see `setTags`
        tagsById[id] = setOf()
        // we don't need to persist since we never store empty tag sets
    }

    suspend fun checkResources(indexedResources: Set<ResourceId>) {
        val knownResources = tagsById.keys.toSet()

        val lostResources = knownResources.subtract(indexedResources)
        if (lostResources.isNotEmpty())
            Log.d(TAGS_STORAGE, "lostResources: $lostResources")

        if (preferences.get(PreferenceKey.RemovingLostResourcesTags))
            lostResources.forEach { resource -> remove(resource) }

        val newResources = indexedResources.subtract(knownResources)
        for (resource in newResources) {
            register(resource)
        }
    }

    suspend fun readStorageIfChanged() {
        if (storageFile.notExists()) return
        if (lastModified != storageFile.getLastModifiedTime()) {
            tagsById.putAll(readStorage())
        }
    }

    override fun contains(id: ResourceId): Boolean = tagsById.containsKey(id)

    // if this id isn't present in storage, then the call is wrong
    // because the caller always takes this id from ResourcesIndex
    // and the index and storage must be in sync
    override fun getTags(id: ResourceId): Tags = tagsById[id]!!
    // todo: check the file's modification date and pull external updates

    override fun getTags(ids: Iterable<ResourceId>): Tags =
        ids.flatMap { id -> getTags(id) }.toSet()

    override suspend fun setTagsAndPersist(id: ResourceId, tags: Tags) =
        withContext(Dispatchers.IO + NonCancellable) {
            setTags(id, tags)
            launch { persist() }
            return@withContext
        }

    suspend fun setTagsAndPersist(
        tagsByIds: Map<ResourceId, Tags>
    ) = withContext(Dispatchers.IO + NonCancellable) {
        tagsByIds.forEach { idToTags ->
            setTags(idToTags.key, idToTags.value)
        }

        launch { persist() }
        return@withContext
    }

    override fun listUntaggedResources(): Set<ResourceId> =
        tagsById
            .filter { (_, tags) -> tags.isEmpty() }
            .keys

    override suspend fun cleanup(existing: Collection<ResourceId>) =
        withContext(Dispatchers.IO) {
            val disappeared = tagsById.keys.minus(existing)

            Log.d(TAGS_STORAGE, "forgetting ${disappeared.size} resources")
            disappeared.forEach { tagsById.remove(it) }
            persist()
        }

    override suspend fun remove(id: ResourceId) = withContext(Dispatchers.IO) {
        Log.d(TAGS_STORAGE, "forgetting resource $id")
        sendStatsEvent(
            StatsEvent.TagsChanged(id, tagsById[id] ?: emptySet(), emptySet())
        )
        tagsById.remove(id)
        persist()
    }

    override fun setTags(id: ResourceId, tags: Tags) {
        if (!tagsById.containsKey(id)) {
            error("Storage isn't aware about this resource id")
        }

        if (tags.isEmpty()) {
            Log.d(
                TAGS_STORAGE,
                "erasing tags for $id and removing the resource"
            )
            tagsById.remove(id)
        }

        Log.d(
            TAGS_STORAGE,
            "new tags for resource $id: $tags"
        )

        sendStatsEvent(
            StatsEvent.TagsChanged(id, tagsById[id] ?: emptySet(), tags)
        )
        tagsById[id] = tags
    }

    override suspend fun persist() =
        withContext(Dispatchers.IO) {
            val exists = Files.exists(storageFile)
            val modified = if (exists) {
                Files.getLastModifiedTime(storageFile)
            } else {
                // storage file could be deleted from outside and, right now,
                // we deal with this case by just re-creating the file,
                // because merging of removals is not implemented yet
                FileTime.fromMillis(0)
                // that also means that we can't drop storage on other device
                // while we have the app running on another one
                // (assume external files synchronization)
            }

            if (modified > lastModified) {
                Log.d(TAGS_STORAGE, "storage file was modified externally, merging")
                // todo: for real merge we need to track our own changes locally
                // without this, we need to stop all competing devices to remove a tag or a resource
                // so far, just ensuring that we are not losing additions

                val outside = readStorage()

                for (newId in outside.keys - tagsById.keys) {
                    Log.d(
                        TAGS_STORAGE,
                        "resource $newId got first tags from outside"
                    )
                    tagsById[newId] = outside[newId]!!
                }

                for (sharedId in outside.keys.intersect(tagsById.keys)) {
                    val theirs = outside[sharedId]
                    val ours = tagsById[sharedId]
                    if (theirs != ours) {
                        Log.d(
                            TAGS_STORAGE,
                            "resource $sharedId got new tags " +
                                "from outside: ${theirs!! - ours}"
                        )
                        tagsById[sharedId] = theirs.union(ours!!)
                    }
                }
            }

            if (tagsById.isEmpty() || tagsById.all { it.value.isEmpty() }) {
                if (exists) {
                    Log.d(TAGS_STORAGE, "no tagged resources, deleting storage file")
                    Files.delete(storageFile)
                }
                return@withContext
            }

            writeStorage()
        }

    private suspend fun readStorage(): Map<ResourceId, Tags> =
        withContext(Dispatchers.IO) {
            val lines = Files.readAllLines(storageFile, StandardCharsets.UTF_8)
            verifyVersion(lines.removeAt(0))

            val result = lines
                .map {
                    val parts = it.split(KEY_VALUE_SEPARATOR)
                    val id = parts[0].toLong()
                    val tags = tagsFromString(parts[1])

                    if (tags.isEmpty()) {
                        throw AssertionError(
                            "Tags storage must not contain empty sets of tags"
                        )
                    }

                    id to tags
                }
                .toMap()

            if (result.isEmpty()) {
                throw AssertionError("Tags storage must not be empty")
            }

            Log.d(TAGS_STORAGE, "${result.size} entries has been read")
            return@withContext result
        }

    private suspend fun writeStorage() =
        withContext(Dispatchers.IO) {
            val lines = mutableListOf<String>()
            lines.add("$STORAGE_VERSION_PREFIX$STORAGE_VERSION")

            val entries = tagsById.filterValues { it.isNotEmpty() }
            lines.addAll(
                entries.map { (id, tags) ->
                    "$id$KEY_VALUE_SEPARATOR ${stringFromTags(tags)}"
                }
            )

            Files.write(storageFile, lines, StandardCharsets.UTF_8)
            lastModified = Files.getLastModifiedTime(storageFile)

            Log.d(TAGS_STORAGE, "${tagsById.size} entries has been written")
        }

    private fun sendStatsEvent(event: StatsEvent) = scope.launch {
        statsFlow.emit(event)
    }

    companion object {
        private const val STORAGE_VERSION = 2
        private const val STORAGE_VERSION_PREFIX = "version "

        const val KEY_VALUE_SEPARATOR = ':'

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
