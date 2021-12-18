package space.taran.arknavigator.mvp.view.item

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.ortiz.touchview.OnTouchImageViewListener
import space.taran.arknavigator.databinding.ItemImageBinding
import space.taran.arknavigator.mvp.model.repo.index.ResourceKind
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.presenter.adapter.PreviewsPagerPresenter
import space.taran.arknavigator.utils.ImageUtils
import space.taran.arknavigator.utils.extensions.makeVisibleAndSetOnClickListener
import java.nio.file.Path

class PreviewItemViewHolder(val binding: ItemImageBinding, val presenter: PreviewsPagerPresenter) :
    RecyclerView.ViewHolder(binding.root),
    PreviewItemView {

    override var pos = -1

    override fun setSource(preview: Path?, placeholder: Int, resource: ResourceMeta) {
        binding.layoutProgress.root.isVisible = false

        ImageUtils.loadZoomImageWithPlaceholder(preview, placeholder, binding.ivImage)

        if (resource.kind == ResourceKind.VIDEO){
            binding.icPlay.makeVisibleAndSetOnClickListener {
                presenter.onPlayButtonClick(pos)
            }
        } else {
            binding.icPlay.isVisible = false
        }

        if (resource.kind == ResourceKind.IMAGE ||
            resource.kind == ResourceKind.DOCUMENT) {
                enableZoom()
        }
    }

    override fun enableZoom(): Unit =
        binding.ivImage.let { view ->
            view.isZoomEnabled = true
            view.setOnTouchImageViewListener(object : OnTouchImageViewListener {
                override fun onMove() {
                    presenter.onImageZoom(view.isZoomed)
                }
            })
        }

    override fun resetZoom() =
        binding.ivImage.resetZoom()
}