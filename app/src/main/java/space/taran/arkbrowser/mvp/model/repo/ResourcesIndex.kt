package space.taran.arkbrowser.mvp.model.repo

import space.taran.arkbrowser.mvp.model.dao.ResourceId
import java.nio.file.Path

interface ResourcesIndex {

    fun listIds(prefix: Path?): List<ResourceId>

    fun getPath(id: ResourceId): Path?

}