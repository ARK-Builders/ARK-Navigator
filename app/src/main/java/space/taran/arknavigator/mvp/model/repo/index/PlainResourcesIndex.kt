package space.taran.arknavigator.mvp.model.repo.index

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.taran.arknavigator.mvp.model.dao.Resource
import space.taran.arknavigator.mvp.model.dao.ResourceDao
import space.taran.arknavigator.mvp.model.dao.ResourceWithExtra
import space.taran.arknavigator.utils.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi

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

        return path
    }

    internal suspend fun reindexRoot(diff: Difference) = withContext(Dispatchers.IO) {
        val savedDeletedMetas = mutableMapOf<Path, ResourceMeta?>()
        diff.deleted.forEach {
            val id = metaByPath[it]!!.id
            pathById[id]
            metaByPath.remove(it)
        }

        Log.d(RESOURCES_INDEX, "deleting ${savedDeletedMetas.size} pdf previews")

        val pathsToDelete = diff.deleted + diff.updated

        Log.d(RESOURCES_INDEX, "removing ${pathsToDelete.size} paths")
        val chunks = pathsToDelete.chunked(512)
        Log.d(RESOURCES_INDEX, "splitting into ${chunks.size} chunks")
        chunks.forEach { paths ->
            dao.deletePaths(paths.map { it.toString() })
        }

        val toInsert = diff.updated + diff.added
        toInsert.forEach { path ->
            val meta = ResourceMeta.fromPath(path)
            metaByPath[path] = meta
            pathById[meta.id] = path
        }

        Log.d(RESOURCES_INDEX, "re-scanning ${toInsert.size} resources")

        //todo: streaming/iterating
        val newResources = scanResources(toInsert)
        persistResources(newResources)
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
            .map {
                Resource.fromMeta(it.value, root, it.key)
            }

        dao.insertResources(entities)

        Log.d(RESOURCES_INDEX, "${entities.size} resources persisted")
    }

    companion object {

        internal suspend fun scanResources(files: List<Path>): Map<Path, ResourceMeta> =
            withContext(Dispatchers.IO) {
                files.map {
                    it to ResourceMeta.fromPath(it)
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