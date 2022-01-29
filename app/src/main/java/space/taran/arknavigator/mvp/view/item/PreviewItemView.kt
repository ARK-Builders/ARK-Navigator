package space.taran.arknavigator.mvp.view.item

import java.nio.file.Path
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta

interface PreviewItemView {
    var pos: Int

    fun setSource(preview: Path?, placeholder: Int, resource: ResourceMeta)
}
