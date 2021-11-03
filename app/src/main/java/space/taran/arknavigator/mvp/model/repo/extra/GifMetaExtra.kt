package space.taran.arknavigator.mvp.model.repo.extra

import space.taran.arknavigator.mvp.model.dao.ResourceExtra
import space.taran.arknavigator.mvp.model.repo.ExtraInfoTag
import space.taran.arknavigator.mvp.model.repo.ResourceMetaExtra
import space.taran.arknavigator.mvp.model.repo.ResourceType

class GifMetaExtra: ResourceMetaExtra(ResourceType.GIF) {
    override val data = mapOf<ExtraInfoTag, String>()

    override fun toRoom(): ResourceExtra {
        TODO("Not yet implemented")
    }
}