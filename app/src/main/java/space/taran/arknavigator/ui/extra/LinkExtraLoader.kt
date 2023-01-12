package space.taran.arknavigator.ui.extra

import android.widget.TextView
import space.taran.arknavigator.R
import space.taran.arklib.domain.kind.ResourceKind
import space.taran.arknavigator.utils.extensions.textOrGone

object LinkExtraLoader {
    fun load(link: ResourceKind.Link, titleTV: TextView, verbose: Boolean) {
        if (!verbose) return
        titleTV.textOrGone(link.title)
    }

    fun loadWithLabel(
        link: ResourceKind.Link,
        titleTV: TextView,
        descriptionTV: TextView,
        linkTv: TextView
    ) {
        titleTV.textOrGone(
            titleTV.context.getString(
                R.string.link_title_label,
                link.title
            )
        )
        link.description?.let {
            descriptionTV.textOrGone(
                descriptionTV.context.getString(
                    R.string.link_description_label,
                    it
                )
            )
        }
        linkTv.textOrGone(linkTv.context.getString(R.string.link_label, link.url))
    }
}
