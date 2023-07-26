package dev.arkbuilders.navigator.stub

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import space.taran.arklib.ResourceId
import space.taran.arklib.domain.meta.Metadata
import space.taran.arklib.domain.meta.MetadataStorage
import space.taran.arklib.domain.meta.MetadataUpdate
import java.nio.file.Path

class MetadataStorageStub : MetadataStorage {
    private val metaById: MutableMap<ResourceId, Metadata> = mapOf(
        R1 to Metadata.PlainText(),
        R2 to Metadata.Image(),
        R3 to Metadata.Video(1, 1, 10),
        R4 to Metadata.Archive()
    ).toMutableMap()

    override fun locate(path: Path, id: ResourceId): Result<Metadata> =
        metaById.mapValues { Result.success(it.value) }
            .getOrDefault(id, Result.failure(IllegalArgumentException()))

    override val inProgress: StateFlow<Boolean>
        get() = TODO("Not yet implemented")

    override val updates: Flow<MetadataUpdate>
        get() = TODO("Not yet implemented")

    override suspend fun forget(id: ResourceId) {
        metaById.remove(id)
    }
}
