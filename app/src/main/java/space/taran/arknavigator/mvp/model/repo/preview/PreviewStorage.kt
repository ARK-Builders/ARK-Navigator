package space.taran.arknavigator.mvp.model.repo.preview

import space.taran.arknavigator.mvp.model.repo.index.ResourceId

interface PreviewStorage {
    fun getPreview(id: ResourceId): Preview
    fun contains(id: ResourceId): Boolean
    suspend fun forget(id: ResourceId)
}