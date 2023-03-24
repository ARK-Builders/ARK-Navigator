package space.taran.arknavigator.mvp.model.repo.tags

import space.taran.arklib.ResourceId
import space.taran.arknavigator.utils.Tags

interface TagsStorage {

    fun contains(id: ResourceId): Boolean

    fun getTags(id: ResourceId): Tags

    fun getTags(ids: Iterable<ResourceId>): Tags

    fun groupTagsByResources(ids: Iterable<ResourceId>): Map<ResourceId, Tags> =
        ids.map { it to getTags(it) }
            .toMap()

    fun setTags(id: ResourceId, tags: Tags)

    suspend fun setTagsAndPersist(id: ResourceId, tags: Tags)

    suspend fun persist()

    fun listUntaggedResources(): Set<ResourceId>

    suspend fun cleanup(existing: Collection<ResourceId>)

    suspend fun remove(id: ResourceId)

    fun isCorrupted(): Boolean
}
