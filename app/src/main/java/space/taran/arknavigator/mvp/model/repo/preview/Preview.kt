package space.taran.arknavigator.mvp.model.repo.preview

import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import java.nio.file.Files
import java.nio.file.Path

class Preview(val preview: Path?, val thumbnail: Path?, val meta: ResourceMeta) {
    init {
        if (preview != null && thumbnail != null) {
            if (Files.exists(preview) && !Files.exists(thumbnail))
                throw AssertionError("Thumbnails must always exist if corresponding preview exists")
        }
    }
}