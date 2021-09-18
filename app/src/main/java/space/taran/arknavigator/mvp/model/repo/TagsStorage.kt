package space.taran.arknavigator.mvp.model.repo

import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.utils.Tag
import space.taran.arknavigator.utils.Tags

interface TagsStorage {

    fun contains(id: ResourceId): Boolean

    fun getTags(id: ResourceId): Tags

    fun getTags(ids: List<ResourceId>): Tags

    fun groupTagsByResources(ids: List<ResourceId>): Map<ResourceId, Tags> {
        val resourcesWithTags = mutableMapOf<ResourceId, Tags>()
        ids.forEach { id ->
            resourcesWithTags[id] = getTags(id)
        }
        return resourcesWithTags
    }

    suspend fun setTags(id: ResourceId, tags: Tags)

    fun listUntaggedResources(): Set<ResourceId>

    suspend fun cleanup(existing: Collection<ResourceId>)

    suspend fun remove(id: ResourceId)

}