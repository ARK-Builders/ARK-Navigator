package space.taran.arknavigator.ui.extra

import android.widget.TextView
import space.taran.arknavigator.mvp.model.repo.kind.ResourceKind
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.utils.extensions.makeGone

object ExtraLoader {
    fun load(meta: ResourceMeta, extraTVs: List<TextView>, verbose: Boolean) {
        extraTVs.forEach { it.makeGone() }

        if (meta.kind == null) return

        when (meta.kind) {
            is ResourceKind.Video -> VideoExtraLoader.load(
                meta.kind,
                extraTVs[0],
                extraTVs[1]
            )
            is ResourceKind.Document -> DocumentExtraLoader.load(
                meta.kind,
                extraTVs[0],
                verbose
            )
            is ResourceKind.Link -> LinkExtraLoader.load(
                meta.kind,
                extraTVs[1],
                verbose
            )
            else -> {}
        }
    }
}
