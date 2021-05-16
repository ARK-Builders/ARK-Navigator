package space.taran.arkbrowser.mvp.model.repo

import space.taran.arkbrowser.mvp.model.entity.room.ResourceId

class AggregatedTagsStorage(
    private val shards: Collection<PlainTagsStorage>)
    : TagsStorage {

    override fun listIds(): Set<ResourceId> {
        return shards
            .flatMap { it.listIds() }
            .toSet()
    }

    override fun removeIds(ids: Collection<ResourceId>) {
        shards.forEach {
            it.removeIds(ids)
        }
    }

}