package space.taran.arkbrowser.mvp.model.repo

import space.taran.arkbrowser.mvp.model.entity.room.ResourceId
import java.nio.file.Path

interface ResourcesIndex {

    fun listIds(prefix: Path?): Set<ResourceId>

}