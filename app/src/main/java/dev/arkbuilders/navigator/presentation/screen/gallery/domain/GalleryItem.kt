package dev.arkbuilders.navigator.presentation.screen.gallery.domain

import dev.arkbuilders.arklib.data.index.Resource
import dev.arkbuilders.arklib.data.meta.Metadata
import dev.arkbuilders.arklib.data.preview.PreviewLocator
import java.nio.file.Path

data class GalleryItem(
    val resource: Resource,
    val preview: PreviewLocator,
    val metadata: Metadata,
    val path: Path
) {
    fun id() = resource.id
}
