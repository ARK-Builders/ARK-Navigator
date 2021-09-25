package space.taran.arknavigator.mvp.presenter.adapter

import space.taran.arknavigator.ui.fragments.utils.Preview
import space.taran.arknavigator.mvp.view.item.PreviewItemView
import space.taran.arknavigator.utils.FileType

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
        } else if (preview.fileType != FileType.PDF){
            view.setImage(preview.previewPath!!)
            view.setZoomEnabled(true)
        }
        else if (preview.fileType == FileType.PDF){
            view.setPDFPreview(preview.previewPath!!)
            view.setZoomEnabled(false)
        }
    }

    fun onImageZoom(zoomed: Boolean) {
        onImageZoomListener(zoomed)
    }

    fun onItemClick(itemView: PreviewItemView) {
        onItemClickListener(itemView)
    }
}