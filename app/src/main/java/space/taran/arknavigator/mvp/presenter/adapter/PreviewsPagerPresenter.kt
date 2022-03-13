package space.taran.arknavigator.mvp.presenter.adapter

import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.preview.PreviewAndThumbnail
import space.taran.arknavigator.mvp.view.GalleryView
import space.taran.arknavigator.mvp.view.item.PreviewItemView
import space.taran.arknavigator.utils.ImageUtils
import space.taran.arknavigator.utils.extension

class PreviewsPagerPresenter(val viewState: GalleryView) {
    private lateinit var index: ResourcesIndex
    private lateinit var onItemClickListener: (PreviewItemView) -> Unit
    private lateinit var onPlayButtonListener: () -> Unit
    private var resources: MutableList<ResourceMeta> = mutableListOf()

    fun init(
        index: ResourcesIndex,
        resources: List<ResourceMeta>,
        onItemClickListener: (PreviewItemView) -> Unit,
        onPlayButtonListener: () -> Unit
    ) {
        this.index = index
        this.resources = resources.toMutableList()
        this.onItemClickListener = onItemClickListener
        this.onPlayButtonListener = onPlayButtonListener
        viewState.updatePagerAdapter()
    }

    fun getCount() = resources.size

    fun remove(position: Int) {
        resources.removeAt(position)
    }

    fun bindView(view: PreviewItemView) {
        val resource = resources[view.pos]
        val path = index.getPath(resource.id)
        val preview = PreviewAndThumbnail.locate(path, resource)?.preview
        val placeholder = ImageUtils.iconForExtension(extension(path))

        view.setSource(preview, placeholder, resource)
    }

    fun onItemClick(itemView: PreviewItemView) {
        onItemClickListener(itemView)
    }

    fun onPlayButtonClick(position: Int) {
        onPlayButtonListener()
    }
}
