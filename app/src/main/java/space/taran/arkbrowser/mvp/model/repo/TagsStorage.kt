package space.taran.arkbrowser.mvp.model.repo

import space.taran.arkbrowser.mvp.model.entity.room.ResourceId
import space.taran.arkbrowser.utils.Tags

interface TagsStorage {

    fun listTags(id: ResourceId): Tags

    fun listResources(): Set<ResourceId>

    fun forgetResources(ids: Collection<ResourceId>)

}