package space.taran.arknavigator.ui.extra

import android.widget.TextView
import space.taran.arknavigator.mvp.model.repo.index.MetaExtraTag
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.utils.extensions.textOrGone

object LinkExtraLoader {
    fun load(meta: ResourceMeta, titleTV: TextView, verbose: Boolean) {
        if (!verbose) return
        titleTV.textOrGone(meta.extra?.data?.get(MetaExtraTag.TITLE))
    }
}
