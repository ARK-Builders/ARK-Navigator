package space.taran.arknavigator.mvp.model.repo.index

import java.nio.file.Path

/**
 * [AggregatedResourceIndex] is useful for "aggregated" navigation mode — a mode in
 * which we navigate through multiple indexed folders (roots). For a single-root
 * navigation [PlainResourceIndex] can be used, and both [AggregatedResourceIndex]
 * and [PlainResourceIndex] should be transparently interchangeable, meaning that
 * both implementations support the same methods and they should not be used
 * otherwise as by [ResourceIndex] interface. Any component using [ResourceIndex]
 * should function in the same fashion independent on implementation of the index,
 * and only capability to look into multiple roots is gained by passing
 * [AggregatedResourceIndex] into the component.
 *
 * @param shards A collection of individual [PlainResourcesIndex] to be aggregated.
 */
class AggregatedResourcesIndex(
    private val shards: Collection<PlainResourcesIndex>
) : ResourcesIndex {

    override suspend fun listResources(prefix: Path?): Set<ResourceMeta> =
        shards.flatMap { it.listResources(prefix) }
            .toSet()

    override suspend fun getPath(id: ResourceId): Path =
        tryShards { it.tryGetPath(id) }

    override suspend fun getMeta(id: ResourceId): ResourceMeta =
        tryShards { it.tryGetMeta(id) }

    override suspend fun remove(id: ResourceId): Path =
        tryShards { it.tryRemove(id) }

    private fun <R> tryShards(f: (shard: PlainResourcesIndex) -> R?): R {
        shards.iterator()
            .forEach { shard ->
                val result = f(shard)
                if (result != null) {
                    return@tryShards result
                }
            }
        throw AssertionError("At least one of shards must yield success")
    }

    override suspend fun reindex() {
        shards.forEach { it.reindex() }
    }

    override suspend fun updateResource(
        oldId: ResourceId,
        path: Path,
        newResource: ResourceMeta
    ) {
        shards.forEach {
            if (it.contains(oldId))
                it.updateResource(oldId, path, newResource)
        }
    }
}
