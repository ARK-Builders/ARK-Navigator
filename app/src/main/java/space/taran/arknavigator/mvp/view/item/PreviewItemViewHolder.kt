package space.taran.arknavigator.mvp.view.item

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.ortiz.touchview.OnTouchImageViewListener
import space.taran.arknavigator.databinding.ItemImageBinding
import space.taran.arknavigator.mvp.model.repo.index.ResourceKind
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.preview.Preview
import space.taran.arknavigator.mvp.presenter.adapter.PreviewsList
import space.taran.arknavigator.utils.ImageUtils
import space.taran.arknavigator.utils.extensions.makeVisibleAndSetOnClickListener
import java.nio.file.Path

class PreviewItemViewHolder(val binding: ItemImageBinding, val presenter: PreviewsList) :
    RecyclerView.ViewHolder(binding.root),
    PreviewItemView {

    override var pos = -1

    override fun setSource(preview: Preview, placeholder: Int) {
        binding.layoutProgress.root.isVisible = false

        ImageUtils.loadImageWithPlaceholder(preview.preview, placeholder, binding.ivImage)

        if (preview.meta.kind == ResourceKind.VIDEO){
            binding.icPlay.makeVisibleAndSetOnClickListener {
                presenter.onPlayButtonCLick(pos)
            }
        } else {
            binding.icPlay.isVisible = false
        }

        if (preview.meta.kind == ResourceKind.IMAGE ||
            preview.meta.kind == ResourceKind.DOCUMENT) {
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