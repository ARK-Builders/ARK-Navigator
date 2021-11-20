package space.taran.arknavigator.mvp.model.repo.extra

import space.taran.arknavigator.mvp.model.repo.index.ResourceMetaExtra
import space.taran.arknavigator.utils.extension
import java.nio.file.Path

object ImageMetaExtra {
    val ACCEPTED_EXTENSIONS: Set<String> =
        setOf("jpg", "jpeg", "png")

    fun extract(path: Path): ResourceMetaExtra? = null

    fun isImage(path: Path): Boolean =
        ACCEPTED_EXTENSIONS.contains(extension(path))
}