package space.taran.arknavigator.mvp.presenter.adapter

import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.view.item.PreviewItemView
import java.nio.file.Path

class PreviewsPresenter {
    private lateinit var onItemClickListener: (PreviewItemView) -> Unit
    private lateinit var onImageZoomListener: (Boolean) -> Unit
    private lateinit var onPlayButtonListener: () -> Unit
    private var previews: MutableList<Path?> = mutableListOf()
    private var placeholders: MutableList<Int> = mutableListOf()
    private var resources: MutableList<ResourceMeta> = mutableListOf()

    fun init(
        previews: List<Path?>,
        placeholders: List<Int>,
        resources: List<ResourceMeta>,
        onItemClickListener: (PreviewItemView) -> Unit,
        onImageZoomListener: (Boolean) -> Unit,
        onPlayButtonListener: () -> Unit
    ) {
        this.previews = previews.toMutableList()
        this.placeholders = placeholders.toMutableList()
        this.resources = resources.toMutableList()
        this.onItemClickListener = onItemClickListener
        this.onImageZoomListener = onImageZoomListener
        this.onPlayButtonListener = onPlayButtonListener
    }

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