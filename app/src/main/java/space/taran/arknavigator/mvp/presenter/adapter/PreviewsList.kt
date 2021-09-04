package space.taran.arknavigator.mvp.presenter.adapter

import space.taran.arknavigator.mvp.model.dao.common.Preview
import space.taran.arknavigator.mvp.view.item.PreviewItemView

typealias PreviewClickHandler = ItemClickHandler<Preview>

class PreviewsList(
    private var previews: List<Preview>,
    private val onItemClickListener: (PreviewItemView) -> Unit,
    private val onImageZoomListener: (Boolean) -> Unit) {

    fun items() = previews

    fun getCount() = previews.size

    fun updateItems(items: List<Preview>) {
        previews = items
    }

    fun bindView(view: PreviewItemView) {
        val preview = previews[view.pos]

        if (preview.predefined != null) {
            view.setPredefined(preview.predefined)
            view.setZoomEnabled(false)
        } else {
            view.setImage(preview.image!!)
            view.setZoomEnabled(true)
        }
    }

    fun onImageZoom(zoomed: Boolean) {
        onImageZoomListener(zoomed)
    }

    fun onItemClick(itemView: PreviewItemView) {
        onItemClickListener(itemView)
    }
}