package dev.arkbuilders.navigator.ui.adapter.previewpager

import space.taran.arklib.ResourceId
import space.taran.arklib.domain.meta.Metadata
import space.taran.arklib.domain.preview.PreviewLocator

interface PreviewItemView {
    var pos: Int

    fun reset()

    fun setSource(
        placeholder: Int,
        id: ResourceId,
        meta: Metadata,
        preview: PreviewLocator
    )
}
