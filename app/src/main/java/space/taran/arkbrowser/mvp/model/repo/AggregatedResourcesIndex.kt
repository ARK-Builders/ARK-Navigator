package space.taran.arkbrowser.mvp.model.repo

import space.taran.arkbrowser.mvp.model.entity.room.ResourceId
import java.nio.file.Path

class AggregatedResourcesIndex(
    private val shards: Collection<PlainResourcesIndex>)
    : ResourcesIndex {

    override fun listIds(prefix: Path?): Set<ResourceId> {
        return shards
            .flatMap { it.listIds(prefix) }
            .toSet()
    }
}