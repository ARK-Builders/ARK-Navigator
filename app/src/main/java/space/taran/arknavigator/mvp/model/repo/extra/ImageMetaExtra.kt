package space.taran.arknavigator.mvp.model.repo.extra

import space.taran.arknavigator.mvp.model.repo.index.ResourceMetaExtra
import java.nio.file.Path

object ImageMetaExtra {
    val ACCEPTED_EXTENSIONS: Set<String> =
        setOf("jpg", "jpeg", "png")

    fun extract(path: Path): ResourceMetaExtra? = null
}