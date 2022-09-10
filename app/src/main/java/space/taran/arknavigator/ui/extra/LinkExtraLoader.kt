package space.taran.arknavigator.ui.extra

import android.widget.TextView
import space.taran.arklib.index.ResourceKind
import space.taran.arknavigator.utils.extensions.textOrGone

object LinkExtraLoader {
    fun load(link: ResourceKind.Link, titleTV: TextView, verbose: Boolean) {
        if (!verbose) return
        titleTV.textOrGone(link.title)
    }
}
