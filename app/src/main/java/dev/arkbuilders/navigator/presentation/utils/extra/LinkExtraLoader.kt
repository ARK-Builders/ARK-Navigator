package dev.arkbuilders.navigator.presentation.utils.extra

import android.widget.TextView
import dev.arkbuilders.navigator.R
import dev.arkbuilders.navigator.presentation.utils.textOrGone
import dev.arkbuilders.arklib.data.meta.Metadata

object LinkExtraLoader {
    fun load(link: Metadata.Link, titleTV: TextView, verbose: Boolean) {
        if (!verbose) return
        titleTV.textOrGone(link.title)
    }

    fun loadWithLabel(
        link: Metadata.Link,
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
        linkTv.textOrGone(linkTv.context.getString(R.string.link_label))
    }
}
