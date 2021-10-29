package space.taran.arknavigator.mvp.presenter.adapter

import space.taran.arknavigator.mvp.model.repo.ResourceMeta
import space.taran.arknavigator.ui.fragments.utils.Preview
import space.taran.arknavigator.mvp.view.item.PreviewItemView

class PreviewsList(
    private var previews: List<Preview>,
    private var resourceMetas: List<ResourceMeta?>?,
    private val onItemClickListener: (PreviewItemView) -> Unit,
    private val onImageZoomListener: (Boolean) -> Unit,
    private val onPlayButtonListener: (Int) -> Unit) {

    fun items() = previews

    fun getCount() = previews.size

    fun updateItems(items: List<Preview>) {
        previews = items
    }

    fun getItem(position: Int): Preview =
        previews[position]

    fun getExtraMetaAt(position: Int): ResourceMeta? =
        resourceMetas?.getOrNull(position)

    fun bindView(view: PreviewItemView) {
        val preview = previews[view.pos]
        val extraMeta = resourceMetas?.getOrNull(view.pos)?.extra

        view.setSource(preview, extraMeta)
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