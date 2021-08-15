package space.taran.arknavigator.mvp.model.repo

import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.utils.Tag
import space.taran.arknavigator.utils.Tags

interface TagsStorage {

    fun getTags(id: ResourceId): Tags

    fun setTags(id: ResourceId, tags: Tags)

    fun listUntaggedResources(): Set<ResourceId>

    fun cleanup(existing: Collection<ResourceId>)

    fun remove(id: ResourceId)

}