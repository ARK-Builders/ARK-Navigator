package space.taran.arknavigator.mvp.model.repo.preview

import space.taran.arknavigator.mvp.model.repo.index.ResourceId

class AggregatedPreviewStorage(private val shards: Collection<PlainPreviewStorage>): PreviewStorage {

    override fun getPreview(id: ResourceId) = shards.find {it.contains(id)}!!.getPreview(id)

    override fun contains(id: ResourceId) = shards.any { it.contains(id) }

    override suspend fun forget(id: ResourceId) = shards.forEach {it.forget(id)}
}