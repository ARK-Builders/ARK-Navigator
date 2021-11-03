package space.taran.arknavigator.mvp.view.item

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.ortiz.touchview.OnTouchImageViewListener
import space.taran.arknavigator.databinding.ItemImageBinding
import space.taran.arknavigator.mvp.model.repo.PreviewsRepo
import space.taran.arknavigator.mvp.model.repo.ResourceMetaExtra
import space.taran.arknavigator.mvp.model.repo.ResourceType
import space.taran.arknavigator.mvp.presenter.adapter.PreviewsList
import space.taran.arknavigator.ui.fragments.utils.Preview
import space.taran.arknavigator.utils.extensions.makeVisibleAndSetOnClickListener
import javax.inject.Inject

class PreviewItemViewHolder(val binding: ItemImageBinding, val presenter: PreviewsList) :
    RecyclerView.ViewHolder(binding.root),
    PreviewItemView {

    override var pos = -1

    @Inject
    lateinit var previewsRepo: PreviewsRepo

    override fun setSource(preview: Preview, extra: ResourceMetaExtra?) {
        binding.layoutProgress.root.isVisible = false
        previewsRepo.loadPreview(
            targetView = binding.ivImage,
            preview = preview,
            extraMeta = extra,
            centerCrop = false
        )

        if (extra?.type != ResourceType.VIDEO){
            binding.icPlay.makeVisibleAndSetOnClickListener {
                presenter.onPlayButtonCLick(pos)
            }
        } else {
            binding.icPlay.isVisible = false
        }

        if (extra?.type == ResourceType.IMAGE ||
            extra?.type == ResourceType.DOCUMENT) {
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