package space.taran.arknavigator.mvp.model.repo.extra

import space.taran.arknavigator.mvp.model.repo.index.ResourceMetaExtra
import java.nio.file.Path

object DocumentMetaExtra {
    val ACCEPTED_EXTENSIONS: Set<String> =
        setOf("pdf", "txt", "doc", "docx", "odt", "ods", "md")

    fun extract(path: Path): ResourceMetaExtra? = null
}
