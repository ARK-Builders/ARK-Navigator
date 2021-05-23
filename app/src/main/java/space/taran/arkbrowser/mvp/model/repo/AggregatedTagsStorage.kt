package space.taran.arkbrowser.mvp.model.repo

import space.taran.arkbrowser.mvp.model.entity.room.ResourceId
import space.taran.arkbrowser.utils.Tags

class AggregatedTagsStorage(
    private val shards: Collection<PlainTagsStorage>)
    : TagsStorage {

    // if we have several copies of a resource across shards,
    // then we receive all tags for the resource
    override fun listTags(id: ResourceId): Tags =
        shards
            .flatMap { it.listTags(id) }
            .toSet()

    override fun listResources(): Set<ResourceId> =
        shards
            .flatMap { it.listResources() }
            .toSet()

    override fun forgetResources(ids: Collection<ResourceId>) =
        shards.forEach {
            it.forgetResources(ids)
        }

}