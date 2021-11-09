package space.taran.arknavigator.mvp.presenter.adapter

import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.preview.Preview
import space.taran.arknavigator.mvp.view.item.PreviewItemView
import java.nio.file.Path

class PreviewsList(
    private val previews: MutableList<Preview>,
    private val placeholders: MutableList<Int>,
    private val onItemClickListener: (PreviewItemView) -> Unit,
    private val onImageZoomListener: (Boolean) -> Unit,
    private val onPlayButtonListener: (Int) -> Unit) {

    fun getCount() = previews.size

    fun remove(position: Int) {
        previews.removeAt(position)
        placeholders.removeAt(position)
    }

    fun bindView(view: PreviewItemView) {
        val preview = previews[view.pos]
        val placeholder = placeholders[view.pos]

        view.setSource(preview, placeholder)
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