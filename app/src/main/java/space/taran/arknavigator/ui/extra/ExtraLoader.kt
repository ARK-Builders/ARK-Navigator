package space.taran.arknavigator.ui.extra

import android.widget.TextView
import space.taran.arklib.domain.meta.Metadata
import space.taran.arknavigator.utils.extensions.makeGone

object ExtraLoader {
    fun load(meta: Metadata, extraTVs: List<TextView>, verbose: Boolean) {
        extraTVs.forEach { it.makeGone() }

        when (meta) {
            is Metadata.Video -> VideoExtraLoader.load(
                meta,
                extraTVs[0],
                extraTVs[1]
            )
            is Metadata.Document -> DocumentExtraLoader.load(
                meta,
                extraTVs[0],
                verbose
            )
            is Metadata.Link -> LinkExtraLoader.load(
                meta,
                extraTVs[1],
                verbose
            )
            else -> {}
        }
    }

    fun loadWithLabel(
        meta: Metadata,
        kindPlaceholders: List<TextView>
    ) {
        require(kindPlaceholders.size == 3)

        kindPlaceholders.forEach { it.makeGone() }

        when (meta) {
            is Metadata.Video -> VideoExtraLoader.loadInfo(
                meta,
                kindPlaceholders[0],
                kindPlaceholders[1]
            )
            is Metadata.Document -> DocumentExtraLoader.loadWithLabel(
                meta,
                kindPlaceholders[0]
            )
            is Metadata.Link -> LinkExtraLoader.loadWithLabel(
                meta,
                kindPlaceholders[0],
                kindPlaceholders[1],
                kindPlaceholders[2]
            )
            else -> {}
        }
    }
}
