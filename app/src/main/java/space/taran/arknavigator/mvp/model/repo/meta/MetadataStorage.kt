package space.taran.arknavigator.mvp.model.repo.meta

import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import java.nio.file.Path

interface MetadataStorage {

    fun locate(path: Path, resource: ResourceMeta): ResourceMeta

    fun forget(id: ResourceId)

    fun generate(path: Path, meta: ResourceMeta)
}
