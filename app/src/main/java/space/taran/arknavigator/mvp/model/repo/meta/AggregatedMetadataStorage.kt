package space.taran.arknavigator.mvp.model.repo.meta

import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.kind.MetaExtraTag
import java.nio.file.Path

class AggregatedMetadataStorage(
    private val shards: Collection<PlainMetadataStorage>
) : MetadataStorage {

    override fun locate(path: Path, resource: ResourceMeta): Map<MetaExtraTag, String>? {
        shards.forEach { shard ->
            shard.locate(path, resource)?.let {
                return it
            }
        }
        return null
    }

    override fun forget(id: ResourceId) = shards.forEach {
        it.forget(id)
    }

    override fun generate(
        path: Path,
        extras: Map<MetaExtraTag, String>,
        meta: ResourceMeta
    ) = shards
        .find { shard -> path.startsWith(shard.root) }
        .let {
            require(it != null) { "At least one of shards must yield success" }
            it.generate(path, extras, meta)
        }
}
