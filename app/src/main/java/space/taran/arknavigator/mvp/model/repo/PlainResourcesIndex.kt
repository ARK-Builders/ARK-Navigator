package space.taran.arknavigator.mvp.model.repo

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.taran.arknavigator.mvp.model.dao.Resource
import space.taran.arknavigator.mvp.model.dao.ResourceDao
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.utils.*
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.listDirectoryEntries

internal data class Difference(
    val deleted: List<Path>,
    val updated: List<Path>,
    val added: List<Path>
)

@OptIn(ExperimentalPathApi::class)
// The index must read from the DAO only during application startup,
// since DB doesn't change from outside. But we must persist all changes
// during application lifecycle into the DAO for the case of any unexpected exit.
class PlainResourcesIndex internal constructor (
    private val root: Path,
    private val dao: ResourceDao,
    resources: Map<Path, ResourceMeta>)
    : ResourcesIndex {

    internal val metaByPath: MutableMap<Path, ResourceMeta> =
        resources.toMutableMap()

    private val pathById: MutableMap<ResourceId, Path> =
        resources.map { (path, meta) ->
            meta.id to path
        }
        .toMap()
        .toMutableMap()

    override fun listIds(prefix: Path?): Set<ResourceId> {
        val ids = if (prefix != null) {
            metaByPath.filterKeys { it.startsWith(prefix) }
        } else {
            metaByPath
        }
        .values
        .map { it.id }

        Log.d(RESOURCES_INDEX, "${ids.size} of ids returned, " +
            "checksum is ${ids.foldRight(0L, { id, acc -> acc + id })}")
        return ids.toSet()
    }

    override fun getPath(id: ResourceId): Path? = pathById[id]

    override fun getMeta(id: ResourceId): ResourceMeta? {
        val path = getPath(id)
        return if (path != null) {
            metaByPath[path]
        } else {
            null
        }
    }

    override fun remove(id: ResourceId): Path? {
        Log.d(RESOURCES_INDEX, "forgetting resource $id")
        val path = pathById.remove(id)
        val idRemoved = metaByPath.remove(path)!!.id

        if (id != idRemoved) {
            throw AssertionError("internal mappings are diverged")
        }

        return path
    }

    internal suspend fun reindexRoot(diff: Difference) = withContext(Dispatchers.IO) {
        diff.deleted.forEach {
            pathById[metaByPath[it]!!.id]
            metaByPath.remove(it)
        }

        val pathsToDelete = diff.deleted + diff.updated

        Log.d(RESOURCES_INDEX, "removing ${pathsToDelete.size} paths")
        val chunks = pathsToDelete.chunked(512)
        Log.d(RESOURCES_INDEX, "splitting into ${chunks.size} chunks")
        chunks.forEach { paths ->
            dao.deletePaths(paths.map { it.toString() })
        }

        val toInsert = diff.updated + diff.added
        toInsert.forEach {
            val meta = ResourceMeta.fromPath(it)
            metaByPath[it] = meta
            pathById[meta.id] = it
        }

        generatePdfPreviews()

        Log.d(RESOURCES_INDEX, "re-scanning ${toInsert.size} resources")

        //todo: streaming/iterating
        val newResources = scanResources(toInsert)
        persistResources(newResources)
    }

    private fun generatePdfPreviews() {
        val previewsFolder = getPdfPreviewsFolder()
        val savedPreviews = getSavedPdfPreviews()

        metaByPath.forEach {
            val path = it.key
            var out: FileOutputStream? = null
            val id = it.value.id

            if (savedPreviews == null || !savedPreviews.contains(id)) {
                if (isPDF(path) && it.value.size / MEGABYTE >= 10) {
                    try {
                        if (!previewsFolder.exists()) previewsFolder.mkdirs()

                        val file = File(previewsFolder, "$id.png")
                        out = FileOutputStream(file)
                        createPdfPreview(path)
                            .compress(Bitmap.CompressFormat.PNG, 100, out)
                    } catch (e: Exception) {
                    } finally {
                        try {
                            out?.close()
                        } catch (e: Exception) {
                        }
                    }
                }
            }
        }
    }

    internal suspend fun calculateDifference(): Difference = withContext(Dispatchers.IO) {
        val (present, absent) = metaByPath.keys.partition {
            Files.exists(it)
        }

        val updated = present
            .map { it to metaByPath[it]!! }
            .filter { (path, meta) ->
                Files.getLastModifiedTime(path) > meta.modified
            }
            .map { (path, _) -> path }

        val added = listAllFiles(root).filter { file ->
            !metaByPath.containsKey(file)
        }

        Log.d(RESOURCES_INDEX, "${absent.size} absent, " +
                "${updated.size} updated, ${added.size} added")

        Difference(absent, updated, added)
    }

    internal suspend fun persistResources(resources: Map<Path, ResourceMeta>) = withContext(Dispatchers.IO) {
        Log.d(RESOURCES_INDEX, "persisting "
                + "${resources.size} resources from root $root")

        val entities = resources.entries.toList()
            .map { Resource.fromMeta(it.value, root, it.key) }

        dao.insertAll(entities)

        Log.d(RESOURCES_INDEX, "${entities.size} resources persisted")
    }

    companion object {

        internal suspend fun scanResources(files: List<Path>): Map<Path, ResourceMeta> =
            withContext(Dispatchers.IO) {
                files.map {
                    it to ResourceMeta.fromPath(it)
                }.toMap()
            }

        internal fun groupResources(resources: List<Resource>): Map<Path, ResourceMeta> =
            resources
                .groupBy { resource -> resource.path }
                .mapValues { (_, resources) ->
                    if (resources.size > 1) {
                        throw IllegalStateException("Index must not have" +
                                "several resources for the same path")
                    }
                    ResourceMeta.fromRoom(resources[0])
                }
                .mapKeys { (path, _) -> Paths.get(path) }

        internal suspend fun listAllFiles(folder: Path): List<Path> = withContext(Dispatchers.IO) {
            val (directories, files) = folder
                .listDirectoryEntries()
                .filter { !isHidden(it) }
                .partition { Files.isDirectory(it) }

            return@withContext files + directories.flatMap {
                listAllFiles(it)
            }
        }
    }
}