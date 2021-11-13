package space.taran.arknavigator.ui.extra

import android.widget.TextView
import space.taran.arknavigator.mvp.model.repo.index.MetaExtraTag
import space.taran.arknavigator.mvp.model.repo.index.ResourceMetaExtra
import space.taran.arknavigator.utils.extensions.textOrGone

object DocumentExtraLoader {
    fun load(extra: ResourceMetaExtra, pagesTV: TextView, verbose: Boolean) {
        val pages = extra.data[MetaExtraTag.PAGES]
        if (pages != null) {
            val label = if (verbose) "$pages pages" else "$pages"
            pagesTV.textOrGone(label)
        }
    }
}