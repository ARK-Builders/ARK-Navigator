package dev.arkbuilders.navigator.stub

import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.domain.tags.TagStorage
import dev.arkbuilders.arklib.domain.tags.Tags

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
