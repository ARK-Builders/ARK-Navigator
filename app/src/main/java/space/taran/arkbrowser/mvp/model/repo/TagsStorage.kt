package space.taran.arkbrowser.mvp.model.repo

import space.taran.arkbrowser.mvp.model.dao.ResourceId
import space.taran.arkbrowser.utils.Tag
import space.taran.arkbrowser.utils.Tags

interface TagsStorage {

    fun getTags(id: ResourceId): Tags

    fun setTags(id: ResourceId, tags: Tags)

    fun listTaggedResources(): Set<ResourceId>

    fun cleanup(existing: Collection<ResourceId>)

}