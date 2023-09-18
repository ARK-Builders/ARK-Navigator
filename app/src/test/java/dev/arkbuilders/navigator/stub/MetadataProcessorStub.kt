package dev.arkbuilders.navigator.stub

import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.data.meta.Metadata
import dev.arkbuilders.arklib.data.meta.MetadataUpdate
import dev.arkbuilders.arklib.data.processor.RootProcessor

class MetadataProcessorStub : RootProcessor<Metadata, MetadataUpdate>() {
    private val metaById: MutableMap<ResourceId, Metadata> = mapOf(
        R1 to Metadata.PlainText(),
        R2 to Metadata.Image(),
        R3 to Metadata.Video(1, 1, 10),
        R4 to Metadata.Archive()
    ).toMutableMap()

    override suspend fun init() {}

    override fun retrieve(id: ResourceId): Result<Metadata> =
        metaById.mapValues { Result.success(it.value) }
            .getOrDefault(id, Result.failure(IllegalArgumentException()))

    override fun forget(id: ResourceId) {
        metaById.remove(id)
    }
}
