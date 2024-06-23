package dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain

import dev.arkbuilders.arklib.data.index.Resource
import dev.arkbuilders.arklib.data.meta.Metadata
import java.nio.file.Path

data class ShowInfoData(
    val path: Path,
    val resource: Resource,
    val metadata: Metadata
)
