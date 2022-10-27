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
                    if (storageByRoot[root] != null) {
                        storageByRoot[root]!!
                    } else {
                        val fresh = PlainScoreStorage(root, resources)
                        fresh.init()
                        storageByRoot[root] = fresh
                        fresh
                    }
                }
                return@withContext AggregatedScoreStorage(storageShards)
            }
        }

    suspend fun provide(root: Path) = provide(
        RootAndFav(root.toString(), favString = null)
    )
}
