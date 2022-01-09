package space.taran.arknavigator.mvp.model.repo.index

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import space.taran.arknavigator.mvp.model.dao.Resource
import space.taran.arknavigator.mvp.model.dao.ResourceDao
import space.taran.arknavigator.mvp.model.dao.ResourceExtra
import space.taran.arknavigator.mvp.model.dao.ResourceWithExtra
import space.taran.arknavigator.mvp.model.repo.preview.PreviewAndThumbnail
import space.taran.arknavigator.utils.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.system.measureTimeMillis

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

    override fun listResources(prefix: Path?): Set<ResourceMeta> {
        val metas = if (prefix != null) {
            metaByPath.filterKeys { it.startsWith(prefix) }
        } else {
            metaByPath
        }
        .values

        Log.d(RESOURCES_INDEX, "${metas.size} resources returned")
        return metas.toSet()
    }

    override fun getPath(id: ResourceId): Path = tryGetPath(id)!!

    override fun getMeta(id: ResourceId): ResourceMeta = tryGetMeta(id)!!

    override fun remove(id: ResourceId): Path {
        Log.d(RESOURCES_INDEX, "forgetting resource $id")
        return tryRemove(id)!!
    }

    //should be only used in AggregatedResourcesIndex
    fun tryGetPath(id: ResourceId): Path? = pathById[id]

    //should be only used in AggregatedResourcesIndex
    fun tryGetMeta(id: ResourceId): ResourceMeta? {
        val path = tryGetPath(id)
        if (path != null) {
            return metaByPath[path]
        }
        return null
    }

    //should be only used in AggregatedResourcesIndex
    fun tryRemove(id: ResourceId): Path? {
        val path = pathById.remove(id) ?: return null

        val idRemoved = metaByPath.remove(path)!!.id

        if (id != idRemoved) {
            throw AssertionError("internal mappings are diverged")
        }

        val duplicatedResource = metaByPath.entries.find { entry -> entry.value.id == idRemoved }
        duplicatedResource?.let { entry ->
            pathById[entry.value.id] = entry.key
        }

        return path
    }

    internal suspend fun reindexRoot(diff: Difference) = withContext(Dispatchers.IO) {
        Log.d(RESOURCES_INDEX, "deleting ${diff.deleted.size} resources from RAM and previews")
        diff.deleted.forEach {
            val id = metaByPath[it]!!.id
            pathById.remove(id)
            metaByPath.remove(it)
            PreviewAndThumbnail.forget(id)
        }

        val pathsToDelete = diff.deleted + diff.updated

        Log.d(RESOURCES_INDEX, "deleting ${pathsToDelete.size} resources from Room DB")
        val chunks = pathsToDelete.chunked(512)
        Log.d(RESOURCES_INDEX, "splitting into ${chunks.size} chunks")
        chunks.forEach { paths ->
            dao.deletePaths(paths.map { it.toString() })
        }

        val newResources = mutableMapOf<Path, ResourceMeta>()
        val toInsert = diff.updated + diff.added

        val time1 = measureTimeMillis {
            toInsert.forEach { path ->
                val meta = ResourceMeta.fromPath(path)
                if (meta != null) {
                    newResources[path] = meta
                    metaByPath[path] = meta
                    pathById[meta.id] = path
                }
            }
        }
        Log.d(RESOURCES_INDEX, "new resources metadata retrieved in ${time1}ms")

        Log.d(RESOURCES_INDEX, "persisting ${newResources.size} updated resources")
        persistResources(newResources)

        val time2 = measureTimeMillis {
            providePreviews()
        }
        Log.d(PREVIEWS, "previews provided in ${time2}ms")
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

    internal suspend fun providePreviews() = withContext(Dispatchers.IO) {
        Log.d(PREVIEWS, "providing previews/thumbnails for ${metaByPath.size} resources")

        supervisorScope {
            metaByPath.entries.map { (path: Path, meta: ResourceMeta) ->
                async(Dispatchers.IO) {
                    PreviewAndThumbnail.generate(path, meta)
                } to path
            }.forEach { (generateTask, path) ->
                try {
                    generateTask.await()
                } catch (e: Exception) {
                    Log.e(PREVIEWS, "Failed to generate preview/thumbnail for id ${metaByPath[path]?.id} ($path)")
                }
            }
        }
    }

    internal suspend fun persistResources(resources: Map<Path, ResourceMeta>) = withContext(Dispatchers.IO) {
        Log.d(RESOURCES_INDEX, "persisting "
            + "${resources.size} resources from root $root")

        val roomResources = mutableListOf<Resource>()
        val roomExtra = mutableListOf<ResourceExtra>()

        resources.entries.toList()
            .forEach {
                roomResources.add(Resource.fromMeta(it.value, root, it.key))
                roomExtra.addAll(ResourceExtra.fromMetaExtra(it.value.id, it.value.extra))
            }

        dao.insertResources(roomResources)
        dao.insertExtras(roomExtra)

        Log.d(RESOURCES_INDEX, "${resources.size} resources persisted")
    }

    companion object {

        internal suspend fun scanResources(files: List<Path>): Map<Path, ResourceMeta> =
            withContext(Dispatchers.IO) {
                files.mapNotNull {
                    ResourceMeta.fromPath(it)?.let { meta ->
                        it to meta
                    }
                }.toMap()
            }

        internal fun loadResources(resources: List<ResourceWithExtra>): Map<Path, ResourceMeta> =
            resources
                .groupBy { room -> room.resource.path }
                .mapValues { (_, resources) ->
                    if (resources.size > 1) {
                        throw IllegalStateException("Index must not have" +
                                "several resources for the same path")
                    }
                    ResourceMeta.fromRoom(resources[0])
                }
                .mapKeys { (path, _) -> Paths.get(path) }

        internal suspend fun listAllFiles(folder: Path): List<Path> = withContext(Dispatchers.IO) {
            val (directories, files) = listChildren(folder)

            return@withContext files + directories.flatMap {
                listAllFiles(it)
            }
        }
    }
}
