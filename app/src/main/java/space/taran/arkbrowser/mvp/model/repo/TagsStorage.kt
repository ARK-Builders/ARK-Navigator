package space.taran.arkbrowser.mvp.model.repo

import space.taran.arkbrowser.mvp.model.entity.room.ResourceId

interface TagsStorage {

    fun listIds(): Set<ResourceId>

    fun removeIds(ids: Collection<ResourceId>)

}