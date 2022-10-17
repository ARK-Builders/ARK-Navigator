package space.taran.arknavigator.mvp.model.repo.preview

import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import java.nio.file.Path

class AggregatedPreviewStorage(
    private val shards: Collection<PlainPreviewStorage>
) : PreviewStorage {

    override fun locate(path: Path, resource: ResourceMeta): PreviewAndThumbnail? {
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

    override fun store(
        path: Path,
        meta: ResourceMeta
    ) = shards
        .find { shard -> path.startsWith(shard.root) }
        .let {
            require(it != null) { "At least one of shards must yield success" }
            it.store(path, meta)
        }
}
