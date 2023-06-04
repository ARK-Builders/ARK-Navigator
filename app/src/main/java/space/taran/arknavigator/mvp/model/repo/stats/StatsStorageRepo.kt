package space.taran.arknavigator.mvp.model.repo.stats

import space.taran.arklib.domain.index.ResourceIndex
import space.taran.arklib.domain.index.RootIndex
import space.taran.arknavigator.mvp.model.repo.preferences.Preferences
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsStorageRepo @Inject constructor(
//    private val tagsStorageRepo: TagsStorageRepo,
    private val preferences: Preferences
) {
    private val storageByRoot = mutableMapOf<Path, PlainStatsStorage>()

    suspend fun provide(index: ResourceIndex): StatsStorage {
        val roots = index.roots

        return if (roots.size > 1) {
            val shards = roots.map {
                provide(it)
            }

            AggregatedStatsStorage(shards)
        } else {
            val root = roots.iterator().next()
            provide(root)
        }
    }

    fun provide(
        root: RootIndex,
//        tagsStorage: PlainTagsStorage
    ): PlainStatsStorage =
        storageByRoot[root.path] ?: PlainStatsStorage(
            root,
            preferences,
        ).also {
            storageByRoot[root.path] = it
        }
}
