package space.taran.arknavigator.mvp.model.repo.tags

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import space.taran.arkfilepicker.folders.FoldersRepo
import space.taran.arkfilepicker.folders.RootAndFav
import space.taran.arklib.domain.index.ResourceIndexRepo
import space.taran.arknavigator.mvp.model.repo.preferences.Preferences
import space.taran.arknavigator.mvp.model.repo.stats.StatsEvent
import java.nio.file.Path

class TagsStorageRepo(
    private val foldersRepo: FoldersRepo,
    private val indexRepo: ResourceIndexRepo,
    private val preferences: Preferences
) {
    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val storageByRoot = mutableMapOf<Path, PlainTagsStorage>()

    private val mutStatsFlow = MutableSharedFlow<StatsEvent>()
    val statsFlow: SharedFlow<StatsEvent>
        get() = mutStatsFlow

    suspend fun provide(
        rootAndFav: RootAndFav
    ): TagsStorage = withContext(Dispatchers.IO) {
        val roots = foldersRepo.resolveRoots(rootAndFav)

        mutex.withLock {
            val shards = roots.map { root ->
                val index = indexRepo.provide(root)
                val resources = index.allIds()

                if (storageByRoot[root] != null) {
                    val storage = storageByRoot[root]!!
                    storage.checkResources(resources)
                    storage.readStorageIfChanged()
                    storage
                } else {
                    val fresh = PlainTagsStorage(
                        root,
                        resources,
                        mutStatsFlow,
                        scope,
                        preferences
                    )
                    fresh.init()
                    fresh.cleanup(resources)
                    storageByRoot[root] = fresh
                    fresh
                }
            }

            return@withContext AggregatedTagsStorage(shards)
        }
    }

    suspend fun provide(
        root: Path
    ): TagsStorage = provide(
        RootAndFav(root.toString(), favString = null)
    )
}
