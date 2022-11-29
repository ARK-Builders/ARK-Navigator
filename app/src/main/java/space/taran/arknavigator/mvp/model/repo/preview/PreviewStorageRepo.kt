package space.taran.arknavigator.mvp.model.repo.preview

import space.taran.arkfilepicker.folders.FoldersRepo
import space.taran.arkfilepicker.folders.RootAndFav
import java.nio.file.Path

class PreviewStorageRepo(
    private val foldersRepo: FoldersRepo
) {
    private val storageByRoot = mutableMapOf<Path, PlainPreviewStorage>()

    suspend fun provide(rootAndFav: RootAndFav): PreviewStorage {
        val roots = foldersRepo.resolveRoots(rootAndFav)

        val shards = roots.map { root ->
            storageByRoot[root] ?: PlainPreviewStorage(root).also {
                storageByRoot[root] = it
            }
        }

        return AggregatedPreviewStorage(shards)
    }

    suspend fun provide(root: Path): PreviewStorage =
        provide(RootAndFav(root.toString(), null))
}
