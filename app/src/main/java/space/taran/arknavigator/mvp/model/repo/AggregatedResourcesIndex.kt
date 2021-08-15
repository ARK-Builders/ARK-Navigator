package space.taran.arknavigator.mvp.model.repo

import space.taran.arknavigator.mvp.model.dao.ResourceId
import java.nio.file.Path

class AggregatedResourcesIndex(
    private val shards: Collection<PlainResourcesIndex>)
    : ResourcesIndex {

    override fun listIds(prefix: Path?): List<ResourceId> =
        shards.flatMap { it.listIds(prefix) }

    override fun getPath(id: ResourceId): Path? =
        //todo: iterators or streams would optimize out redundant maps
        shards.mapNotNull { it.getPath(id) }
            .firstOrNull()

    override fun remove(id: ResourceId): Path? =
        shards.mapNotNull { it.remove(id) }
            .firstOrNull()
}