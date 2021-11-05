package space.taran.arknavigator.mvp.model.repo.extra

import android.widget.TextView
import space.taran.arknavigator.mvp.model.repo.index.MetaExtraTag
import space.taran.arknavigator.mvp.model.repo.index.ResourceMetaExtra
import space.taran.arknavigator.utils.extensions.textOrGone
import java.nio.file.Path

object DocumentMetaExtra {
    val ACCEPTED_EXTENSIONS: Set<String> =
        setOf("pdf", "txt", "doc", "docx", "odt", "ods", "md")

    fun extract(path: Path): ResourceMetaExtra? = null

    fun draw(extra: ResourceMetaExtra, pagesTV: TextView, verbose: Boolean) {
        val pages = extra.data[MetaExtraTag.PAGES]
        if (pages != null) {
            val label = if (verbose) "$pages pages" else "$pages"
            pagesTV.textOrGone(label)
        }
    }
}