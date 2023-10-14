package dev.arkbuilders.navigator.stub

import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.data.index.Resource
import dev.arkbuilders.arklib.data.index.ResourceIndex
import dev.arkbuilders.arklib.data.index.ResourceUpdates
import dev.arkbuilders.arklib.data.index.RootIndex
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

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
