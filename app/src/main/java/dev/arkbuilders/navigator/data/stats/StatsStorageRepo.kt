package dev.arkbuilders.navigator.data.stats

import kotlinx.coroutines.flow.SharedFlow
import dev.arkbuilders.arklib.domain.index.ResourceIndex
import dev.arkbuilders.arklib.domain.index.RootIndex
import dev.arkbuilders.arklib.domain.stats.StatsEvent
import dev.arkbuilders.arklib.domain.tags.RootTagsStorage
import dev.arkbuilders.arklib.domain.tags.TagsStorageRepo
import dev.arkbuilders.navigator.data.preferences.Preferences
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

    suspend fun provide(
        root: RootIndex,
        tagsStorage: RootTagsStorage
    ): PlainStatsStorage =
        storageByRoot[root.path] ?: PlainStatsStorage(
            root,
            preferences,
            tagsStorage,
            statsFlow
        ).also {
            it.init()
            storageByRoot[root.path] = it
        }
}
