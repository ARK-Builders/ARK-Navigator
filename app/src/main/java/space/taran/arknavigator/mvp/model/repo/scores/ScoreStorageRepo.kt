package space.taran.arknavigator.mvp.model.repo.scores

import space.taran.arklib.domain.index.ResourceIndex
import space.taran.arklib.domain.index.RootIndex
import java.nio.file.Path

class ScoreStorageRepo {
    private val storageByRoot = mutableMapOf<Path, PlainScoreStorage>()

    suspend fun provide(index: ResourceIndex): ScoreStorage {
        val roots = index.roots

        return if (roots.size > 1) {
            val shards = roots.map { provide(it) }

            AggregatedScoreStorage(shards)
        } else {
            val root = roots.iterator().next()
            provide(root)
        }
    }

    suspend fun provide(root: RootIndex): PlainScoreStorage =
        storageByRoot[root.path] ?: PlainScoreStorage(
            root.path,
            root.allIds(),
        ).also {
            storageByRoot[root.path] = it
        }
}
