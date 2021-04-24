package space.taran.arkbrowser.mvp.model.repo

import space.taran.arkbrowser.mvp.model.entity.room.Resource
import java.io.File
import space.taran.arkbrowser.mvp.model.entity.room.ResourceId
import space.taran.arkbrowser.mvp.model.entity.room.computeId
import space.taran.arkbrowser.mvp.model.entity.room.dao.ResourceDao
import space.taran.arkbrowser.utils.Timestamp
import space.taran.arkbrowser.utils.listAllFiles
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

private typealias MutableResources = MutableMap<File, ResourceMeta>

private data class ResourceMeta(val id: ResourceId, val modified: Timestamp)

private data class Difference(
    val deleted: List<File>,
    val updated: List<File>,
    val added: List<File>)

class ResourcesIndex(private val resourceDao: ResourceDao) {

    // Initializing the index from application database
    private val rootToResources: MutableMap<File, MutableResources> =
        resourceDao.getAll()
            .groupBy { it.root }
            .mapValues { it.value
                .groupBy { resource -> resource.path }
                .mapValues { (_, resources) ->
                    if (resources.size > 1) {
                        throw IllegalStateException("Database must not have" +
                            "several resources for the same path")
                    }
                    val resource = resources[0]
                    ResourceMeta(resource.id, resource.modified)
                }
                .mapKeys { (path, _) -> File(path) }
                .toMutableMap()
            }
            .mapKeys { (root, _) -> File(root) }
            .toMutableMap()

    init {
        // The index is initialized, scanning for modifications since last run
        rootToResources.keys.forEach { reindexRoot(it, calculateDifference(it)) }
    }

    fun indexRoot(root: File) {
        if (rootToResources.containsKey(root)) {
            reindexRoot(root, calculateDifference(root))
            return
        }

        val resources = indexFiles(root, listAllFiles(root))
        rootToResources[root] = resources.toMutableMap()
    }

    private fun reindexRoot(root: File, diff: Difference) {
        val pathToMeta = rootToResources[root]!!

        diff.deleted.forEach {
            pathToMeta.remove(it)
        }
        (diff.deleted + diff.updated).forEach {
            resourceDao.deleteByPath(it.path)
        }

        val toInsert = diff.updated + diff.added
        toInsert.forEach {
            pathToMeta[it] = ResourceMeta(
                id = computeId(it),
                modified = it.lastModified())
        }

        indexFiles(root, toInsert)
    }

    private fun indexFiles(root: File, files: List<File>): Map<File, ResourceMeta> {
        val resources = files.map {
            it to ResourceMeta(
                id = computeId(it),
                modified = it.lastModified())
        }.toMap()

        resourceDao.insertAll(
            resources.entries.toList()
                .map { Resource(
                    id = it.value.id,
                    root = root.path,
                    path = it.key.path,
                    modified = it.value.modified)
                })

        return resources
    }

    private fun calculateDifference(root: File): Difference {
        val pathToMeta = rootToResources[root]
            ?: throw IllegalArgumentException("Root $root isn't indexed yet")

        val (present, absent) = pathToMeta.keys.partition { it.exists() }

        val updated = present
            .map { it to pathToMeta[it]!! }
            .filter { (path, meta) ->
                path.lastModified() > meta.modified
            }
            .map { (path, _) -> path }

        val added = listAllFiles(root).filter { file ->
            !pathToMeta.containsKey(file)
        }

        return Difference(absent, updated, added)
    }
}