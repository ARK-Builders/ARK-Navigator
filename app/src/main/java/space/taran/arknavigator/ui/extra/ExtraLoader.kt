package space.taran.arknavigator.ui.extra

import android.widget.TextView
import space.taran.arknavigator.mvp.model.repo.index.ResourceKind
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.utils.extensions.makeGone

object ExtraLoader {
    fun load(meta: ResourceMeta, extraTVs: List<TextView>, verbose: Boolean) {
        extraTVs.forEach { it.makeGone() }

        if (meta.extra == null) return

        when (meta.kind) {
            ResourceKind.VIDEO -> VideoExtraLoader.load(
                meta.extra,
                extraTVs[0],
                extraTVs[1]
            )
            ResourceKind.DOCUMENT -> DocumentExtraLoader.load(
                meta.extra,
                extraTVs[0],
                verbose
            )
            ResourceKind.LINK -> LinkExtraLoader.load(
                meta,
                extraTVs[1],
                verbose
            )
            else -> {}
        }
    }
}
