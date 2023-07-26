package dev.arkbuilders.navigator.stub

import space.taran.arklib.ResourceId
import dev.arkbuilders.navigator.mvp.model.repo.tags.TagsStorage
import dev.arkbuilders.navigator.utils.Tags

class TagsStorageStub : TagsStorage {
    private val tagsById = TestData.tagsById().toMutableMap()

    override fun contains(id: ResourceId): Boolean =
        tagsById.contains(id)

    override fun getTags(id: ResourceId): Tags = tagsById[id]!!

    override fun getTags(ids: Iterable<ResourceId>): Tags = ids.flatMap { id ->
        getTags(id)
    }.toSet()

    override fun setTags(id: ResourceId, tags: Tags) {
        tagsById[id] = tags
    }

    override suspend fun setTagsAndPersist(id: ResourceId, tags: Tags) {
        tagsById[id] = tags
    }

    override suspend fun persist() {}

    override fun listUntaggedResources(): Set<ResourceId> = tagsById
        .filter { (_, tags) -> tags.isEmpty() }
        .keys

    override suspend fun cleanup(existing: Collection<ResourceId>) {}

    override suspend fun remove(id: ResourceId) {
        tagsById.remove(id)
    }

    override fun isCorrupted() = false
}
