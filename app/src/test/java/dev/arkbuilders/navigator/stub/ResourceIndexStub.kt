package dev.arkbuilders.navigator.stub

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.domain.index.Resource
import dev.arkbuilders.arklib.domain.index.ResourceIndex
import dev.arkbuilders.arklib.domain.index.ResourceUpdates
import dev.arkbuilders.arklib.domain.index.RootIndex

import java.nio.file.Path
import kotlin.io.path.Path

class ResourceIndexStub : ResourceIndex {
    private val resources = TestData.resourceById().toMutableMap()

    override val roots: Set<RootIndex> = setOf()

    override val updates: Flow<ResourceUpdates> =
        MutableSharedFlow()

    override suspend fun updateAll() {}

    override fun allResources(): Map<ResourceId, Resource> =
        resources.toMap()

    override fun getResource(id: ResourceId): Resource? =
        resources[id]

    override fun allPaths(): Map<ResourceId, Path> =
        resources.mapValues { Path("") }

    override fun getPath(id: ResourceId): Path? =
        Path("")
}
