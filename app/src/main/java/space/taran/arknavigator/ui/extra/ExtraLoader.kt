package space.taran.arknavigator.ui.extra

import android.widget.TextView
import space.taran.arklib.domain.index.ResourceMeta
import space.taran.arklib.domain.kind.ResourceKind
import space.taran.arknavigator.utils.extensions.makeGone

object ExtraLoader {
    fun load(meta: ResourceMeta, extraTVs: List<TextView>, verbose: Boolean) {
        extraTVs.forEach { it.makeGone() }

        if (meta.kind == null) return

        when (meta.kind) {
            is ResourceKind.Video -> VideoExtraLoader.load(
                meta.kind as ResourceKind.Video,
                extraTVs[0],
                extraTVs[1]
            )
            is ResourceKind.Document -> DocumentExtraLoader.load(
                meta.kind as ResourceKind.Document,
                extraTVs[0],
                verbose
            )
            is ResourceKind.Link -> LinkExtraLoader.load(
                meta.kind as ResourceKind.Link,
                extraTVs[1],
                verbose
            )
            else -> {}
        }
    }

    fun loadWithLabel(
        meta: ResourceMeta,
        kindPlaceholders: List<TextView>
    ) {
        require(kindPlaceholders.size == 3)

        kindPlaceholders.forEach { it.makeGone() }

        if (meta.kind == null) return

        when (meta.kind) {
            is ResourceKind.Video -> VideoExtraLoader.loadInfo(
                meta.kind as ResourceKind.Video,
                kindPlaceholders[0],
                kindPlaceholders[1]
            )
            is ResourceKind.Document -> DocumentExtraLoader.loadWithLabel(
                meta.kind as ResourceKind.Document,
                kindPlaceholders[0]
            )
            is ResourceKind.Link -> LinkExtraLoader.loadWithLabel(
                meta.kind as ResourceKind.Link,
                kindPlaceholders[0],
                kindPlaceholders[1],
                kindPlaceholders[2]
            )
            else -> {}
        }
    }
}
