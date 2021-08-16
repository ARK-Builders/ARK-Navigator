package space.taran.arknavigator.mvp.model.repo

import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.utils.Tag
import space.taran.arknavigator.utils.Tags

class AggregatedTagsStorage(
    private val shards: Collection<PlainTagsStorage>)
    : TagsStorage {

    // if we have several copies of a resource across shards,
    // then we would receive all tags for the resource. but
    // copies of the same resource under different roots
    // are forbidden now
    override fun getTags(id: ResourceId): Tags =
        shards
            .flatMap { it.getTags(id) }
            .toSet()

    override suspend fun setTags(id: ResourceId, tags: Tags) =
        shards.forEach {
            it.setTags(id, tags)
        }

    // assuming that resources in the shards do not overlap
    override fun listUntaggedResources(): Set<ResourceId> =
        shards
            .flatMap { it.listUntaggedResources() }
            .toSet()

    override suspend fun cleanup(existing: Collection<ResourceId>) =
        shards.forEach {
            it.cleanup(existing)
        }

    override suspend fun remove(id: ResourceId) =
        shards.forEach {
            it.remove(id)
        }

}