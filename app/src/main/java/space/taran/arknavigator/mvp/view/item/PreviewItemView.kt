package space.taran.arknavigator.mvp.view.item

import space.taran.arknavigator.mvp.model.repo.ResourceMetaExtra
import space.taran.arknavigator.ui.fragments.utils.Preview

interface PreviewItemView {
    var pos: Int

    fun setSource(preview: Preview, extraMeta: ResourceMetaExtra?)

    fun setZoomEnabled(enabled: Boolean)

    fun resetZoom()
}