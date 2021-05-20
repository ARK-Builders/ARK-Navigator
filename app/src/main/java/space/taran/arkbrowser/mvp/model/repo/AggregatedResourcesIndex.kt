package space.taran.arkbrowser.mvp.model.repo

import space.taran.arkbrowser.mvp.model.entity.room.ResourceId
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
}