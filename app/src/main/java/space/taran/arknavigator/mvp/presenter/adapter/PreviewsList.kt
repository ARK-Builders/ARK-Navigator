package space.taran.arknavigator.mvp.presenter.adapter

import space.taran.arknavigator.mvp.model.repo.ResourceMetaExtra
import space.taran.arknavigator.mvp.view.item.PreviewItemView
import java.nio.file.Path

class PreviewsList(
    private val previews: MutableList<Path?>,
    private val placeholders: MutableList<Int>,
    private val extras: MutableList<ResourceMetaExtra?>,
    private val onItemClickListener: (PreviewItemView) -> Unit,
    private val onImageZoomListener: (Boolean) -> Unit,
    private val onPlayButtonListener: (Int) -> Unit) {

    fun getCount() = previews.size

    fun remove(position: Int) {
        previews.removeAt(position)
        placeholders.removeAt(position)
        extras.removeAt(position)
    }

    fun bindView(view: PreviewItemView) {
        val preview = previews[view.pos]
        val placeholder = placeholders[view.pos]
        val extra = extras[view.pos]

        view.setSource(preview, placeholder, extra)
    }

    fun onImageZoom(zoomed: Boolean) {
        onImageZoomListener(zoomed)
    }

    fun onItemClick(itemView: PreviewItemView) {
        onItemClickListener(itemView)
    }

    fun onPlayButtonCLick(position: Int) {
        onPlayButtonListener(position)
    }
}