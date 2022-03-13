package space.taran.arknavigator.mvp.model.repo.extra

import space.taran.arknavigator.mvp.model.repo.index.ResourceMetaExtra
import java.nio.file.Path

object ArchiveMetaExtra {
    val ACCEPTED_EXTENSIONS: Set<String> =
        setOf("zip")

    fun extract(path: Path): ResourceMetaExtra? = null
}
