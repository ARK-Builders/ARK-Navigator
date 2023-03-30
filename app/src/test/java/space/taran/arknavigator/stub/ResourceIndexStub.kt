package space.taran.arknavigator.stub

import space.taran.arklib.ResourceId
import space.taran.arklib.domain.index.Resource
import space.taran.arklib.domain.index.ResourceIndex
import java.nio.file.Path
import kotlin.io.path.Path

class ResourceIndexStub : ResourceIndex {
    private val resources = TestData.resourceById().toMutableMap()

    override suspend fun getPath(id: ResourceId): Path =
        Path("")

    override suspend fun allResources(prefix: Path?): Set<Resource> =
        resources.values.toSet()

    override suspend fun getResource(id: ResourceId): Resource =
        resources[id]!!

    override suspend fun updateAll() {}
}
