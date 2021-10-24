package space.taran.arknavigator.mvp.model.repo

import android.util.Log
import space.taran.arknavigator.mvp.model.dao.ResourceExtra
import space.taran.arknavigator.ui.fragments.utils.Preview
import space.taran.arknavigator.utils.getVideoInfo
import java.nio.file.Path

data class VideoMetaExtra(
    override var filePath: Path? = null
): ResourceMetaExtra() {

    override fun appData(): MutableMap<Preview.ExtraInfoTag, String> {
        return getVideoInfo(filePath)
    }

    override fun roomData(): ResourceExtra {
        return false
    }
}