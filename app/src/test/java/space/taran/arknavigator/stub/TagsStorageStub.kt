package space.taran.arknavigator.stub

import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorage
import space.taran.arknavigator.utils.Tags

class TagsStorageStub : TagsStorage {
    private val tagsById = TestData.tagsById().toMutableMap()

    override fun contains(id: ResourceId): Boolean =
        tagsById.contains(id)

    override fun getTags(id: ResourceId): Tags = tagsById[id]!!

    override fun getTags(ids: Iterable<ResourceId>): Tags = ids.flatMap { id ->
        getTags(id)
    }.toSet()

    override suspend fun setTags(id: ResourceId, tags: Tags) {
        tagsById[id] = tags
    }

    override fun listUntaggedResources(): Set<ResourceId> = tagsById
        .filter { (_, tags) -> tags.isEmpty() }
        .keys

    override suspend fun cleanup(existing: Collection<ResourceId>) {}

    override suspend fun remove(id: ResourceId) {
        tagsById.remove(id)
    }
}
