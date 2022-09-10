package space.taran.arknavigator.mvp.model.repo.index

import android.util.Log
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import space.taran.arklib.index.ResourceMeta
import space.taran.arklib.index.RustResourcesIndex
import space.taran.arknavigator.mvp.model.dao.Resource
import space.taran.arknavigator.mvp.model.dao.ResourceDao
import space.taran.arknavigator.mvp.model.dao.ResourceExtra
import space.taran.arknavigator.mvp.model.dao.ResourceWithExtra
import space.taran.arknavigator.mvp.model.repo.kind.GeneralKindFactory
import space.taran.arknavigator.mvp.model.repo.preview.PreviewAndThumbnail
import space.taran.arknavigator.utils.LogTags.PREVIEWS
import space.taran.arknavigator.utils.LogTags.RESOURCES_INDEX
import space.taran.arknavigator.utils.listChildren

@OptIn(ExperimentalPathApi::class)
// The index must read from the DAO only during application startup,
// since DB doesn't change from outside. But we must persist all changes
// during application lifecycle into the DAO for the case of any unexpected exit.
class PlainResourcesIndex
internal constructor(
    private val root: Path,
    private val dao: ResourceDao,
    resources: Map<Path, ResourceMeta>
) : ResourcesIndex {

    private val ri = RustResourcesIndex(root.toString(), resources.toMutableMap())

    override suspend fun listResources(prefix: Path?): Set<ResourceMeta> =
        ri.listResources(prefix?.toString()).values.toSet()

    fun contains(id: ResourceId): Boolean = ri.contains(id)

    override suspend fun getPath(id: ResourceId): Path = tryGetPath(id)!!

    override suspend fun getMeta(id: ResourceId): ResourceMeta = tryGetMeta(id)!!

    override suspend fun remove(id: ResourceId): Path {
        Log.d(RESOURCES_INDEX, "forgetting resource $id")
        return tryRemove(id)!!
    }

    override suspend fun reindex() {
        reindexRoot()
    }

    // should be only used in AggregatedResourcesIndex
    fun tryGetPath(id: ResourceId): Path? = ri.getPath(id)

    // should be only used in AggregatedResourcesIndex
    fun tryGetMeta(id: ResourceId): ResourceMeta? {
        return ri.getMeta(id)
    }

    // should be only used in AggregatedResourcesIndex
    fun tryRemove(id: ResourceId): Path? {
        return ri.remove(id)
    }

    private suspend fun reindexRoot() {
        withContext(Dispatchers.IO) {
            val diff = ri.reindex()
            Log.d(
                RESOURCES_INDEX,
                "deleting ${diff.deleted.size} resources from RAM and previews"
            )

            diff.deleted.forEach { PreviewAndThumbnail.forget(it.value.id) }

            //  TODO Replace with rusty deleted(already included deleted and updated)
            val pathsToDelete = diff.deleted
            Log.d(
                RESOURCES_INDEX,
                "deleting ${pathsToDelete.size} resources from Room DB"
            )

            val chunks = pathsToDelete.keys.chunked(512)
            Log.d(RESOURCES_INDEX, "splitting into ${chunks.size} chunks")
            chunks.forEach { paths -> dao.deletePaths(paths.map { it.toString() }) }

            val newResources = mutableMapOf<Path, ResourceMeta>()

            val toInsert = diff.added

            val time1 = measureTimeMillis {
                toInsert.forEach { it ->
                    val meta = ResourceMetaExtra.fromPath(it.key)
                    ri.updateResource(it.key, meta!!)
                }
            }
            Log.d(
                RESOURCES_INDEX,
                "new resources metadata retrieved in ${time1}ms"
            )

            Log.d(
                RESOURCES_INDEX,
                "persisting ${newResources.size} updated resources"
            )
            persistResources(newResources)

            val time2 = measureTimeMillis { providePreviews() }
            Log.d(
                PREVIEWS,
                "previews provided in ${time2}ms"
            )
        }
    }

    private suspend fun providePreviews() {
        withContext(Dispatchers.IO) {
            val metaByPath = ri.listResources("")
            Log.d(
                PREVIEWS,
                "providing previews/thumbnails for ${metaByPath.size} resources"
            )
            PreviewAndThumbnail.initDirs()

            supervisorScope {
                metaByPath.entries
                    .map { (path: Path, meta: ResourceMeta) ->
                        async(Dispatchers.IO) {
                            PreviewAndThumbnail.generate(path, meta)
                        } to path
                    }
                    .forEach { (generateTask, path) ->
                        try {
                            generateTask.await()
                        } catch (e: Exception) {
                            Log.e(
                                PREVIEWS,
                                "Failed to generate preview/thumbnail for id ${
                                metaByPath[path]?.id
                                } ($path)"
                            )
                        }
                    }
            }
        }
    }

    internal suspend fun persistResources(resources: Map<Path, ResourceMeta>) =
        withContext(Dispatchers.IO) {
            Log.d(
                RESOURCES_INDEX,
                "persisting " + "${resources.size} resources from root $root"
            )

            val roomResources = mutableListOf<Resource>()
            val roomExtra = mutableListOf<ResourceExtra>()

            resources.entries.toList().forEach {
                roomResources.add(
                    Resource.fromMeta(it.value, root, it.key)
                )
                roomExtra.addAll(
                    GeneralKindFactory.toRoom(it.value.id, it.value.kind)
                )
            }

            dao.insertResources(roomResources)
            dao.insertExtras(roomExtra)

            Log.d(RESOURCES_INDEX, "${resources.size} resources persisted")
        }

    override suspend fun updateResource(
        oldId: ResourceId,
        path: Path,
        newResource: ResourceMeta
    ) {
        ri.updateResource(path, newResource)
        dao.updateResource(
            oldId,
            newResource.id,
            newResource.modified.toMillis(),
            newResource.size
        )
        dao.updateExtras(oldId, newResource.id)
    }

    companion object {

        internal suspend fun scanResources(
            files: List<Path>
        ): Map<Path, ResourceMeta> =
            withContext(Dispatchers.IO) {
                Log.d(RESOURCES_INDEX, "scanning resources $files")

                files
                    .mapNotNull {
                        ResourceMetaExtra.fromPath(it)?.let { meta -> it to meta }
                    }
                    .toMap()
            }

        internal fun loadResources(
            resources: List<ResourceWithExtra>
        ): Map<Path, ResourceMeta> =
            resources
                .groupBy { room -> room.resource.path }
                .mapValues { (_, resources) ->
                    if (resources.size > 1) {
                        throw IllegalStateException(
                            "Index must not have" +
                                "several resources for the same path"
                        )
                    }
                    ResourceMetaExtra.fromRoom(resources[0])
                }
                .mapKeys { (path, _) -> Paths.get(path) }

        internal suspend fun listAllFiles(folder: Path): List<Path> =
            withContext(Dispatchers.IO) {
                val (directories, files) = listChildren(folder)

                return@withContext files + directories.flatMap { listAllFiles(it) }
            }
    }
}

internal data class Difference(
    val deleted: List<Path>,
    val updated: List<Path>,
    val added: List<Path>
)
