package space.taran.arknavigator.mvp.model.repo

import space.taran.arknavigator.mvp.model.dao.ResourceExtra
import space.taran.arknavigator.ui.fragments.utils.Preview
import space.taran.arknavigator.utils.getVideoInfo
import java.nio.file.Path

class VideoMetaExtra: ResourceMetaExtra() {

    override var appData = mutableMapOf<Preview.ExtraInfoTag, String>()

    override fun appData(filePath: Path?): MutableMap<Preview.ExtraInfoTag, String> {
        if (appData.isNullOrEmpty()) appData = getVideoInfo(filePath)
        return appData
    }

    override fun roomData(): ResourceExtra {
        return false
    }
}