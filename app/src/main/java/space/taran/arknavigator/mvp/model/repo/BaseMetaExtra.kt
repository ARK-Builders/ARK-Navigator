package space.taran.arknavigator.mvp.model.repo

import space.taran.arknavigator.mvp.model.dao.ResourceExtra
import space.taran.arknavigator.ui.fragments.utils.Preview
import java.nio.file.Path

class BaseMetaExtra: ResourceMetaExtra() {

    override var appData = mutableMapOf<Preview.ExtraInfoTag, String>()

    override fun appData(filePath: Path?): MutableMap<Preview.ExtraInfoTag, String> {
        return appData
    }

    override fun roomData(): ResourceExtra {
        return false
    }
}