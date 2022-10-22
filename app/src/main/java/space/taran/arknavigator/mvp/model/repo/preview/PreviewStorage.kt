package space.taran.arknavigator.mvp.model.repo.preview

import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import java.nio.file.Path

interface PreviewStorage {

    fun locate(path: Path, resource: ResourceMeta): PreviewAndThumbnail?

    fun forget(id: ResourceId)

    fun store(path: Path, meta: ResourceMeta)
}
