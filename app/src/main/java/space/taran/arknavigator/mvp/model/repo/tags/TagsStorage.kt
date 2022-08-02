package space.taran.arknavigator.mvp.model.repo.tags

import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.utils.Tags
import java.nio.file.Path

interface TagsStorage {

    fun roots(): List<Path>

    fun contains(id: ResourceId): Boolean

    fun getTags(id: ResourceId): Tags

    fun getTags(ids: Iterable<ResourceId>): Tags

    fun groupTagsByResources(ids: Iterable<ResourceId>): Map<ResourceId, Tags> =
        ids.map { it to getTags(it) }
            .toMap()

    suspend fun setTagsAndPersist(id: ResourceId, tags: Tags)

    fun listUntaggedResources(): Set<ResourceId>

    suspend fun cleanup(existing: Collection<ResourceId>)

    suspend fun remove(id: ResourceId)
}
