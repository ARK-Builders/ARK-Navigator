package space.taran.arknavigator.ui.extra

import android.widget.TextView
import space.taran.arklib.index.ResourceKind
import space.taran.arknavigator.utils.extensions.textOrGone

object DocumentExtraLoader {
    fun load(document: ResourceKind.Document, pagesTV: TextView, verbose: Boolean) {
        val pages = document.pages
        if (pages != null) {
            val label = when {
                verbose -> {
                    if (pages.toInt() == 1) "$pages page"
                    else "$pages pages"
                }
                else -> "$pages"
            }
            pagesTV.textOrGone(label)
        }
    }
}
