package space.taran.arknavigator.mvp.presenter.adapter

import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.view.item.PreviewItemView
import java.nio.file.Path

class PreviewsList(
    previews: List<Path?>,
    placeholders: List<Int>,
    resources: List<ResourceMeta>,
    private val onItemClickListener: (PreviewItemView) -> Unit,
    private val onImageZoomListener: (Boolean) -> Unit,
    private val onPlayButtonListener: () -> Unit) {

    private val previews: MutableList<Path?> = previews.toMutableList()
    private val placeholders: MutableList<Int> = placeholders.toMutableList()
    private val resources: MutableList<ResourceMeta> = resources.toMutableList()

    fun getCount() = previews.size

    fun remove(position: Int) {
        previews.removeAt(position)
        placeholders.removeAt(position)
        resources.removeAt(position)
    }

    fun bindView(view: PreviewItemView) {
        val preview = previews[view.pos]
        val placeholder = placeholders[view.pos]
        val resource = resources[view.pos]

        view.setSource(preview, placeholder, resource)
    }

    fun onImageZoom(zoomed: Boolean) {
        onImageZoomListener(zoomed)
    }

    fun onItemClick(itemView: PreviewItemView) {
        onItemClickListener(itemView)
    }

    fun onPlayButtonClick(position: Int) {
        onPlayButtonListener()
    }
}