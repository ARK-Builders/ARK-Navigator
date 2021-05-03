package space.taran.arkbrowser.mvp.model.repo

import space.taran.arkbrowser.mvp.model.entity.room.Resource
import java.io.File
import space.taran.arkbrowser.mvp.model.entity.room.ResourceId
import space.taran.arkbrowser.mvp.model.entity.room.computeId
import space.taran.arkbrowser.mvp.model.entity.room.ResourceDao
import space.taran.arkbrowser.utils.Timestamp
import space.taran.arkbrowser.utils.listAllFiles
import java.lang.IllegalStateException

data class ResourceMeta(val id: ResourceId, val modified: Timestamp)

private data class Difference(
    val deleted: List<File>,
    val updated: List<File>,
    val added: List<File>)

// The index must read from the DAO only during application startup,
// since DB doesn't change from outside. But we must persist all changes
// during application lifecycle into the DAO for the case of any unexpected exit.
class ResourcesIndex private constructor (
    private val root: File,
    private val dao: ResourceDao,
    resources: Map<File, ResourceMeta>) {

    private val pathToMeta: MutableMap<File, ResourceMeta> =
        resources.toMutableMap()

    //todo query functions

    //todo modification functions with immediate persisting

    companion object {
        // Constructor for loading from the database
        fun loadFromDatabase(
                root: File,
                dao: ResourceDao
        ): ResourcesIndex {
            //todo https://www.toptal.com/android/android-threading-all-you-need-to-know
            // Use Case #7: Querying local SQLite database

            val index = ResourcesIndex(root, dao,
                groupResources(dao.query()))

            index.reindexRoot(index.calculateDifference())
            return index
        }

        // Constructor for building from the filesystem
        fun buildFromFilesystem(
                root: File,
                dao: ResourceDao
        ): ResourcesIndex {
            val index = ResourcesIndex(root, dao,
                scanResources(listAllFiles(root)))

            index.persistResources(index.pathToMeta)
            return index
        }

        //todo: parallel and asynchronous
        private fun scanResources(files: List<File>): Map<File, ResourceMeta> =
            files.map {
                it to ResourceMeta(
                    id = computeId(it),
                    modified = it.lastModified())
            }.toMap()

        private fun groupResources(resources: List<Resource>): Map<File, ResourceMeta> =
            resources
                .groupBy { resource -> resource.path }
                .mapValues { (_, resources) ->
                    if (resources.size > 1) {
                        throw IllegalStateException("Index must not have" +
                                "several resources for the same path")
                    }
                    val resource = resources[0]
                    ResourceMeta(resource.id, resource.modified)
                }
                .mapKeys { (path, _) -> File(path) }
    }

    private fun reindexRoot(diff: Difference) {
        diff.deleted.forEach {
            pathToMeta.remove(it)
        }
        (diff.deleted + diff.updated).forEach {
            dao.deleteByPath(it.path)
        }

        val toInsert = diff.updated + diff.added
        toInsert.forEach {
            pathToMeta[it] = ResourceMeta(
                id = computeId(it),
                modified = it.lastModified())
        }

        val newResources = scanResources(toInsert)
        persistResources(newResources)
    }

    private fun calculateDifference(): Difference {
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

    private fun persistResources(resources: Map<File, ResourceMeta>) {
        dao.insertAll(
            resources.entries.toList()
                .map { Resource(
                    id = it.value.id,
                    root = root.path,
                    path = it.key.path,
                    modified = it.value.modified)
                })
    }
}