package space.taran.arknavigator.mvp.model.repo.scores

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import space.taran.arknavigator.mvp.model.repo.FoldersRepo
import space.taran.arknavigator.mvp.model.repo.RootAndFav
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndexRepo
import java.nio.file.Path

class ScoreStorageRepo(
    private val foldersRepo: FoldersRepo,
    private val indexRepo: ResourcesIndexRepo
) {
    private val provideMutex = Mutex()
    private val storageByRoot = mutableMapOf<Path, PlainScoreStorage>()

    suspend fun provide(rootAndFav: RootAndFav): ScoreStorage =
        withContext(Dispatchers.IO) {
            val roots = foldersRepo.resolveRoots(rootAndFav)

            provideMutex.withLock {
                val storageShards = roots.map { root ->
                    val index = indexRepo.provide(root)
                    val resources = index.listAllIds()
                    val resMeta = resources.map { index.getMeta(it) }
                    if (storageByRoot[root] != null) {
                        storageByRoot[root]?.refresh(resMeta)
                        storageByRoot[root]!!
                    } else {
                        val scoreStorage = PlainScoreStorage(root, resources)
                        scoreStorage.init()
                        storageByRoot[root] = scoreStorage
                        scoreStorage
                    }
                }
                return@withContext AggregatedScoreStorage(storageShards)
            }
        }

    suspend fun provide(root: Path) = provide(
        RootAndFav(root.toString(), favString = null)
    )
}
