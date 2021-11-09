package space.taran.arknavigator.mvp.model.repo.preview

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndex
import java.nio.file.Path

class PreviewStorageRepo {
    private val storageByRoot = mutableMapOf<Path, PlainPreviewStorage>()

    suspend fun provide(root: Path, index: ResourcesIndex): PlainPreviewStorage = withContext(Dispatchers.IO) {
        val storage = storageByRoot[root] ?: PlainPreviewStorage(root)
        storage.init(index)
        return@withContext storage
    }
}