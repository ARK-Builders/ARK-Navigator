package space.taran.arknavigator.mvp.model.repo.tags

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import space.taran.arknavigator.mvp.model.repo.FoldersRepo
import space.taran.arknavigator.mvp.model.repo.RootAndFav
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndexRepo
import space.taran.arknavigator.mvp.model.repo.preferences.Preferences
import java.nio.file.Path

class TagsStorageRepo(
    private val foldersRepo: FoldersRepo,
    private val indexRepo: ResourcesIndexRepo,
    private val preferences: Preferences
) {
    private val provideMutex = Mutex()
    private val storageByRoot = mutableMapOf<Path, PlainTagsStorage>()

    suspend fun provide(rootAndFav: RootAndFav): TagsStorage =
        withContext(Dispatchers.IO) {
            val roots = foldersRepo.resolveRoots(rootAndFav)

            provideMutex.withLock {
                val storageShards = roots.map { root ->
                    val index = indexRepo.provide(root)
                    val resources = index.listAllIds()
                    if (storageByRoot[root] != null) {
                        val storage = storageByRoot[root]!!
                        storage.checkResources(resources)
                        storage
                    } else {
                        val fresh =
                            PlainTagsStorage(root, resources, preferences)
                        fresh.init()
                        fresh.cleanup(resources)
                        storageByRoot[root] = fresh
                        fresh
                    }
                }

                return@withContext AggregatedTagsStorage(storageShards)
            }
        }

    suspend fun provide(root: Path): TagsStorage =
        provide(RootAndFav(root.toString(), favString = null))
}
