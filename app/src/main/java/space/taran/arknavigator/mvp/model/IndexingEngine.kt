package space.taran.arknavigator.mvp.model

import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.repo.FoldersRepo
import space.taran.arknavigator.mvp.model.repo.ResourcesIndexFactory
import java.nio.file.Files
import java.nio.file.Path

class IndexingEngine(
    private val indexCache: IndexCache,
    private val tagsCache: TagsCache,
    private val foldersRepo: FoldersRepo,
    private val resourcesIndexFactory: ResourcesIndexFactory
) {
    suspend fun reindex() {
        val roots = foldersRepo.query().succeeded.keys
        roots.forEach {
            val index = resourcesIndexFactory.loadFromDatabase(it)
            tagsCache.onIndexChanged(it, index)
            indexCache.onIndexChange(it, index)
        }
        indexCache.onReindexFinish()
        tagsCache.onReindexFinish()
    }

    suspend fun index(path: Path) {
        val index = resourcesIndexFactory.buildFromFilesystem(path)
        indexCache.onIndexChange(path, index)
        tagsCache.onIndexChanged(path, index)
    }

    suspend fun remove(resourceId: ResourceId): Path? {
        val path = indexCache.remove(resourceId)
        tagsCache.remove(resourceId)
        Files.delete(path)
        return path
    }
}