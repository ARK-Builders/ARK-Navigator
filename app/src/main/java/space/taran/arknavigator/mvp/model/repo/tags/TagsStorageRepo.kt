package dev.arkbuilders.navigator.mvp.model.repo.tags

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import space.taran.arklib.domain.index.ResourceIndex
import space.taran.arklib.domain.index.RootIndex
import dev.arkbuilders.navigator.mvp.model.repo.preferences.Preferences
import dev.arkbuilders.navigator.mvp.model.repo.stats.StatsEvent
import java.nio.file.Path

class TagsStorageRepo(
    private val preferences: Preferences
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val storageByRoot = mutableMapOf<Path, PlainTagsStorage>()

    private val mutStatsFlow = MutableSharedFlow<StatsEvent>()
    val statsFlow: SharedFlow<StatsEvent>
        get() = mutStatsFlow

    suspend fun provide(index: ResourceIndex): TagsStorage {
        val roots = index.roots

        return if (roots.size > 1) {
            val shards = roots.map { provide(it) }

            AggregatedTagsStorage(shards)
        } else {
            val root = roots.iterator().next()
            provide(root)
        }
    }

    suspend fun provide(root: RootIndex): PlainTagsStorage =
        storageByRoot[root.path] ?: PlainTagsStorage(
            root.path,
            root.allIds(),
            mutStatsFlow,
            scope,
            preferences
        ).also {
            storageByRoot[root.path] = it
        }
}
