package space.taran.arknavigator.mvp.model.repo.tags

import space.taran.arklib.ResourceId
import space.taran.arknavigator.utils.Tags

class AggregatedTagsStorage(
    val shards: Collection<TagsStorage>
) : TagsStorage {

    override fun contains(id: ResourceId): Boolean =
        shards.any { it.contains(id) }

    // if we have several copies of a resource across shards,
    // then we would receive all tags for the resource. but
    // copies of the same resource under different roots
    // are forbidden now
    override fun getTags(id: ResourceId): Tags =
        shards
            .flatMap {
                if (it.contains(id)) it.getTags(id)
                else emptySet()
            }
            .toSet()

    override fun getTags(ids: Iterable<ResourceId>): Tags =
        ids.flatMap { id -> getTags(id) }.toSet()

    override fun setTags(id: ResourceId, tags: Tags) {
        shards.forEach {
            if (it.contains(id))
                it.setTags(id, tags)
        }
    }

    override suspend fun persist() = shards.forEach { it.persist() }

    override suspend fun setTagsAndPersist(id: ResourceId, tags: Tags) =
        shards.forEach {
            if (it.contains(id))
                it.setTagsAndPersist(id, tags)
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
