package space.taran.arknavigator.mvp.model.repo.index

import android.util.Log
import java.nio.file.Path
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import space.taran.arknavigator.mvp.model.dao.ResourceDao
import space.taran.arknavigator.mvp.model.repo.FoldersRepo
import space.taran.arknavigator.mvp.model.repo.RootAndFav
import space.taran.arknavigator.mvp.model.repo.index.PlainResourcesIndex.Companion.listAllFiles
import space.taran.arknavigator.mvp.model.repo.index.PlainResourcesIndex.Companion.loadResources
import space.taran.arknavigator.mvp.model.repo.index.PlainResourcesIndex.Companion.scanResources
import space.taran.arknavigator.utils.LogTags.RESOURCES_INDEX

class ResourcesIndexRepo(
    private val dao: ResourceDao,
    private val foldersRepo: FoldersRepo
) {
    private val provideMutex = Mutex()
    private val indexByRoot = mutableMapOf<Path, PlainResourcesIndex>()

    suspend fun loadFromDatabase(
        root: Path,
        kindDetectFailedFlow: MutableSharedFlow<Path>? = null
    ): PlainResourcesIndex =
        withContext(Dispatchers.IO) {
            Log.d(
                RESOURCES_INDEX,
                "loading index for $root from the database"
            )

            val resources = dao.query(root.toString())

            Log.d(
                RESOURCES_INDEX,
                "${resources.size} resources retrieved from DB"
            )

            val index = PlainResourcesIndex(root, dao, loadResources(resources))
            Log.d(RESOURCES_INDEX, "index created")
            index.reindex(kindDetectFailedFlow)
            indexByRoot[root] = index
            return@withContext index
        }

    suspend fun buildFromFilesystem(
        root: Path,
        kindDetectFailedFlow: MutableSharedFlow<Path>? = null
    ): PlainResourcesIndex =
        withContext(Dispatchers.IO) {
            Log.d(RESOURCES_INDEX, "building index from root $root")

            var files: List<Path>

            val time1 = measureTimeMillis {
                files = listAllFiles(root)
            }
            Log.d(
                RESOURCES_INDEX,
                "listed ${files.size} files in $time1 milliseconds"
            )

            var metadata: Map<Path, ResourceMeta>

            val time2 = measureTimeMillis {
                metadata = scanResources(files, kindDetectFailedFlow)
            }
            Log.d(
                RESOURCES_INDEX,
                "resources metadata retrieved in $time2 milliseconds"
            )

            val index = PlainResourcesIndex(root, dao, metadata)

            index.persistResources(index.metaByPath)
            indexByRoot[root] = index
            return@withContext index
        }

    suspend fun provide(
        rootAndFav: RootAndFav,
        kindDetectFailedFlow: MutableSharedFlow<Path>? = null
    ): ResourcesIndex =
        withContext(Dispatchers.IO) {
            val roots = foldersRepo.resolveRoots(rootAndFav)

            provideMutex.withLock {
                val indexShards = roots.map { root ->
                    indexByRoot[root] ?: let {
                        val index = loadFromDatabase(root, kindDetectFailedFlow)
                        indexByRoot[root] = index
                        index
                    }
                }

                return@withContext AggregatedResourcesIndex(indexShards)
            }
        }

    suspend fun provide(
        root: Path,
        kindDetectFailedFlow: MutableSharedFlow<Path>? = null
    ): ResourcesIndex = provide(
        RootAndFav(root.toString(), favString = null),
        kindDetectFailedFlow
    )

    suspend fun isIndexed(rootAndFav: RootAndFav): Boolean {
        val roots = foldersRepo.resolveRoots(rootAndFav)
        roots.forEach { root ->
            if (!indexByRoot.contains(root))
                return false
        }
        return true
    }
}
