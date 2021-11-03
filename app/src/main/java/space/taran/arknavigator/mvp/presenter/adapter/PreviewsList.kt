package space.taran.arknavigator.mvp.presenter.adapter

import space.taran.arknavigator.mvp.model.repo.ResourceMetaExtra
import space.taran.arknavigator.ui.fragments.utils.Preview
import space.taran.arknavigator.mvp.view.item.PreviewItemView

class PreviewsList(
    private var previews: List<Preview>,
    private var extras: List<ResourceMetaExtra?>,
    private val onItemClickListener: (PreviewItemView) -> Unit,
    private val onImageZoomListener: (Boolean) -> Unit,
    private val onPlayButtonListener: (Int) -> Unit) {

    fun items() = previews

    fun getCount() = previews.size

    fun updateItems(items: List<Preview>) {
        previews = items
    }

    fun bindView(view: PreviewItemView) {
        val preview = previews[view.pos]
        val extra = extras[view.pos]

        view.setSource(preview, extra)
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