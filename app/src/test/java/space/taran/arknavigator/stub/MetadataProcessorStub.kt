package space.taran.arknavigator.stub

import space.taran.arklib.ResourceId
import space.taran.arklib.domain.meta.Metadata
import space.taran.arklib.domain.meta.MetadataUpdate
import space.taran.arklib.domain.processor.RootProcessor

class MetadataProcessorStub : RootProcessor<Metadata, MetadataUpdate>() {
    private val metaById: MutableMap<ResourceId, Metadata> = mapOf(
        R1 to Metadata.PlainText(),
        R2 to Metadata.Image(),
        R3 to Metadata.Video(1, 1, 10),
        R4 to Metadata.Archive()
    ).toMutableMap()

    override fun retrieve(id: ResourceId): Result<Metadata> =
        metaById.mapValues { Result.success(it.value) }
            .getOrDefault(id, Result.failure(IllegalArgumentException()))

    override fun forget(id: ResourceId) {
        metaById.remove(id)
    }
}
