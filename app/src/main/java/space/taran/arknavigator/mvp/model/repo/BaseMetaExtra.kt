package space.taran.arknavigator.mvp.model.repo

import space.taran.arknavigator.mvp.model.dao.ResourceExtra
import space.taran.arknavigator.ui.fragments.utils.Preview
import java.nio.file.Path

class BaseMetaExtra(override var filePath: Path?) : ResourceMetaExtra() {

    override fun appData(): MutableMap<Preview.ExtraInfoTag, String> {
        return mutableMapOf()
    }

    override fun roomData(): ResourceExtra {
        return false
    }
}