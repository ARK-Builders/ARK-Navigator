package dev.arkbuilders.navigator.stub

import space.taran.arklib.ResourceId
import space.taran.arklib.domain.tags.TagStorage
import space.taran.arklib.domain.tags.Tags

class TagsStorageStub : TagStorage {
    private val tagsById = TestData.tagsById().toMutableMap()

    override fun getValue(id: ResourceId) = tagsById[id]!!

    override suspend fun persist() {}

    override fun remove(id: ResourceId) {
        tagsById.remove(id)
    }

    override fun setValue(
        id: ResourceId,
        value: Tags
    ) {
        tagsById[id] = value
    }
}
