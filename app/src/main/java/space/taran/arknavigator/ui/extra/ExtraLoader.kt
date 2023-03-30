package space.taran.arknavigator.ui.extra

import android.widget.TextView
import space.taran.arklib.domain.index.Resource
import space.taran.arklib.domain.kind.Metadata
import space.taran.arknavigator.utils.extensions.makeGone

object ExtraLoader {
    fun load(resource: Resource, extraTVs: List<TextView>, verbose: Boolean) {
        extraTVs.forEach { it.makeGone() }

        if (resource.metadata == null) return

        when (resource.metadata) {
            is Metadata.Video -> VideoExtraLoader.load(
                resource.metadata as Metadata.Video,
                extraTVs[0],
                extraTVs[1]
            )
            is Metadata.Document -> DocumentExtraLoader.load(
                resource.metadata as Metadata.Document,
                extraTVs[0],
                verbose
            )
            is Metadata.Link -> LinkExtraLoader.load(
                resource.metadata as Metadata.Link,
                extraTVs[1],
                verbose
            )
            else -> {}
        }
    }

    fun loadWithLabel(
        resource: Resource,
        kindPlaceholders: List<TextView>
    ) {
        require(kindPlaceholders.size == 3)

        kindPlaceholders.forEach { it.makeGone() }

        if (resource.metadata == null) return

        when (resource.metadata) {
            is Metadata.Video -> VideoExtraLoader.loadInfo(
                resource.metadata as Metadata.Video,
                kindPlaceholders[0],
                kindPlaceholders[1]
            )
            is Metadata.Document -> DocumentExtraLoader.loadWithLabel(
                resource.metadata as Metadata.Document,
                kindPlaceholders[0]
            )
            is Metadata.Link -> LinkExtraLoader.loadWithLabel(
                resource.metadata as Metadata.Link,
                kindPlaceholders[0],
                kindPlaceholders[1],
                kindPlaceholders[2]
            )
            else -> {}
        }
    }
}
