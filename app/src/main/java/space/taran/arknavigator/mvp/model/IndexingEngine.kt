package space.taran.arknavigator.mvp.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.taran.arknavigator.mvp.model.fsmonitoring.FSMonitoring
import space.taran.arknavigator.mvp.model.repo.FoldersRepo
import space.taran.arknavigator.mvp.model.repo.ResourcesIndexFactory
import java.nio.file.Path

class IndexingEngine(
    private val indexCache: IndexCache,
    private val tagsCache: TagsCache,
    private val foldersRepo: FoldersRepo,
    private val resourcesIndexFactory: ResourcesIndexFactory,
    private val fsMonitoring: FSMonitoring
) {
    suspend fun reindex() = withContext(Dispatchers.Default) {
        val roots = foldersRepo.query().succeeded.keys
        roots.forEach {
            val index = resourcesIndexFactory.loadFromDatabase(it)
            tagsCache.onIndexChanged(it, index)
            indexCache.onIndexChange(it, index)
            fsMonitoring.startWatchingRoot(it.toString())
        }
        indexCache.onReindexFinish()
        tagsCache.onReindexFinish()
    }

    suspend fun index(root: Path) = withContext(Dispatchers.Default)  {
        val index = resourcesIndexFactory.buildFromFilesystem(root)
        indexCache.onIndexChange(root, index)
        tagsCache.onIndexChanged(root, index)
        fsMonitoring.startWatchingRoot(root.toString())
    }
}