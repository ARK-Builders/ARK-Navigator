package space.taran.arkbrowser.mvp.model.repo

import space.taran.arkbrowser.mvp.model.dao.ResourceId
import space.taran.arkbrowser.utils.Tag
import space.taran.arkbrowser.utils.Tags

class AggregatedTagsStorage(
    private val shards: Collection<PlainTagsStorage>)
    : TagsStorage {

    // if we have several copies of a resource across shards,
    // then we receive all tags for the resource
    override fun getTags(id: ResourceId): Tags =
        shards
            .flatMap { it.getTags(id) }
            .toSet()

    override fun setTags(id: ResourceId, tags: Tags) =
        shards.forEach {
            it.setTags(id, tags)
        }

    override fun listTaggedResources(): Set<ResourceId> =
        shards
            .flatMap { it.listTaggedResources() }
            .toSet()

    override fun cleanup(existing: Collection<ResourceId>) =
        shards.forEach {
            it.cleanup(existing)
        }

    override fun remove(id: ResourceId) =
        shards.forEach {
            it.remove(id)
        }

}