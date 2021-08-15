package space.taran.arknavigator.mvp.model.repo

import android.util.Log
import space.taran.arknavigator.mvp.model.dao.Resource
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.dao.computeId
import space.taran.arknavigator.mvp.model.dao.ResourceDao
import space.taran.arknavigator.utils.CoroutineRunner
import space.taran.arknavigator.utils.RESOURCES_INDEX
import space.taran.arknavigator.utils.isHidden
import java.lang.AssertionError
import java.lang.IllegalStateException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.FileTime
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.listDirectoryEntries

data class ResourceMeta(val id: ResourceId, val modified: FileTime)

internal data class Difference(
    val deleted: List<Path>,
    val updated: List<Path>,
    val added: List<Path>)

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

    override fun listIds(prefix: Path?): List<ResourceId> {
        val ids = if (prefix != null) {
            metaByPath.filterKeys { it.startsWith(prefix) }
        } else {
            metaByPath
        }
        .values
        .map { it.id }

        Log.d(RESOURCES_INDEX, "${ids.size} of ids returned, " +
            "checksum is ${ids.foldRight(0L, { id, acc -> acc + id })}")
        return ids
    }

    override fun getPath(id: ResourceId): Path? = pathById[id]

    override fun remove(id: ResourceId): Path? {
        Log.d(RESOURCES_INDEX, "forgetting resource $id")
        val path = pathById.remove(id)
        val idRemoved = metaByPath.remove(path)!!.id

        if (id != idRemoved) {
            throw AssertionError("internal mappings are diverged")
        }

        return path
    }

    //todo query functions

    //todo modification functions with immediate persisting

    internal fun reindexRoot(diff: Difference) {
        diff.deleted.forEach {
            pathById[metaByPath[it]!!.id]
            metaByPath.remove(it)
        }

        val pathsToDelete = diff.deleted + diff.updated
        CoroutineRunner.runAndBlock {
            dao.deletePaths(pathsToDelete.map { it.toString() })
        }

        val toInsert = diff.updated + diff.added
        toInsert.forEach {
            val id = computeId(it)

            metaByPath[it] = ResourceMeta(
                id = id,
                modified = Files.getLastModifiedTime(it))
            pathById[id] = it
        }

        Log.d(RESOURCES_INDEX, "re-scanning ${toInsert.size} resources")

        //todo: streaming/iterating
        val newResources = scanResources(toInsert)
        persistResources(newResources)
    }

    internal fun calculateDifference(): Difference {
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

        return Difference(absent, updated, added)
    }

    internal fun persistResources(resources: Map<Path, ResourceMeta>) {
        Log.d(RESOURCES_INDEX, "persisting "
                + "${resources.size} resources from root $root")

        val entities = resources.entries.toList()
            .map { Resource(
                id = it.value.id,
                root = root.toString(),
                path = it.key.toString(),
                modified = it.value.modified.toMillis())
            }

        CoroutineRunner.runAndBlock {
            dao.insertAll(entities)
        }
        Log.d(RESOURCES_INDEX, "${entities.size} resources persisted")
    }

    companion object {
        //todo: parallel and asynchronous
        internal fun scanResources(files: List<Path>): Map<Path, ResourceMeta> =
            files.map {
                it to ResourceMeta(
                    id = computeId(it),
                    modified = Files.getLastModifiedTime(it))
            }
            .toMap()

        internal fun groupResources(resources: List<Resource>): Map<Path, ResourceMeta> =
            resources
                .groupBy { resource -> resource.path }
                .mapValues { (_, resources) ->
                    if (resources.size > 1) {
                        throw IllegalStateException("Index must not have" +
                                "several resources for the same path")
                    }
                    val resource = resources[0]
                    ResourceMeta(
                        id = resource.id,
                        modified = FileTime.fromMillis(resource.modified))
                }
                .mapKeys { (path, _) -> Paths.get(path) }

        internal fun listAllFiles(folder: Path): List<Path> {
            val (directories, files) = folder
                .listDirectoryEntries()
                .filter { !isHidden(it) }
                .partition { Files.isDirectory(it) }

            return files + directories.flatMap {
                listAllFiles(it)
            }
        }
    }
}