package space.taran.arknavigator.mvp.model.repo.extra

import space.taran.arknavigator.mvp.model.dao.ResourceExtra
import space.taran.arknavigator.mvp.model.repo.ExtraInfoTag
import space.taran.arknavigator.mvp.model.repo.ResourceMetaExtra
import space.taran.arknavigator.mvp.model.repo.ResourceType

class PdfMetaExtra: ResourceMetaExtra(ResourceType.PDF) {
    override val data: MutableMap<ExtraInfoTag, String> =
        //TODO: PR #33
        mutableMapOf()

    override fun toRoom(): ResourceExtra {
        TODO("Not yet implemented")
    }
}