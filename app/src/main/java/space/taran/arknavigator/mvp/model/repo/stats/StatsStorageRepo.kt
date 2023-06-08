package space.taran.arknavigator.mvp.model.repo.stats

import kotlinx.coroutines.flow.SharedFlow
import space.taran.arklib.domain.index.ResourceIndex
import space.taran.arklib.domain.index.RootIndex
import space.taran.arklib.domain.stats.StatsEvent
import space.taran.arklib.domain.tags.RootTagsStorage
import space.taran.arklib.domain.tags.TagsStorageRepo
import space.taran.arknavigator.mvp.model.repo.preferences.Preferences
import java.nio.file.Path

class StatsStorageRepo(
    private val tagsStorageRepo: TagsStorageRepo,
    private val preferences: Preferences,
    private val statsFlow: SharedFlow<StatsEvent>
) {
    private val storageByRoot = mutableMapOf<Path, PlainStatsStorage>()

    suspend fun provide(index: ResourceIndex): StatsStorage {
        val roots = index.roots

        return if (roots.size > 1) {
            val shards = roots.map {
                val tagsStorage = tagsStorageRepo.provide(it)
                provide(it, tagsStorage)
            }

            AggregatedStatsStorage(shards)
        } else {
            val root = roots.iterator().next()
            val tagsStorage = tagsStorageRepo.provide(root)
            provide(root, tagsStorage)
        }
    }

    fun provide(
        root: RootIndex,
        tagsStorage: RootTagsStorage
    ): PlainStatsStorage =
        storageByRoot[root.path] ?: PlainStatsStorage(
            root,
            preferences,
            tagsStorage,
            statsFlow
        ).also {
            storageByRoot[root.path] = it
        }
}
