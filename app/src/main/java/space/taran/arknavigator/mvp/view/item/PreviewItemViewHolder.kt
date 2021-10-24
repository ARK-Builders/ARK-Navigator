package space.taran.arknavigator.mvp.view.item

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.ortiz.touchview.OnTouchImageViewListener
import space.taran.arknavigator.databinding.ItemImageBinding
import space.taran.arknavigator.mvp.model.repo.PreviewsRepo
import space.taran.arknavigator.mvp.model.repo.ResourceMetaExtra
import space.taran.arknavigator.mvp.presenter.adapter.PreviewsList
import space.taran.arknavigator.ui.fragments.utils.Preview
import javax.inject.Inject

class PreviewItemViewHolder(val binding: ItemImageBinding, val presenter: PreviewsList) :
    RecyclerView.ViewHolder(binding.root),
    PreviewItemView {

    override var pos = -1

    @Inject
    lateinit var previewsRepo: PreviewsRepo

    override fun setSource(preview: Preview, extraMeta: ResourceMetaExtra?) {
        binding.layoutProgress.root.isVisible = false
        previewsRepo.loadPreview(
            targetView = binding.ivImage,
            preview = preview,
            extraMeta = extraMeta
        )

        binding.icPlay.isVisible = previewsRepo.isPlayButtonVisible(preview)

        setZoomEnabled(previewsRepo.isZoomEnabled(preview, extraMeta))
    }

    override fun setZoomEnabled(enabled: Boolean): Unit =
        binding.ivImage.let { touchImageView ->
            touchImageView.isZoomEnabled = enabled
            if (enabled) {
                touchImageView.setOnTouchImageViewListener(object : OnTouchImageViewListener {
                    override fun onMove() {
                        presenter.onImageZoom(touchImageView.isZoomed)
                    }
                })
            } else {
                touchImageView.setOnTouchImageViewListener(object : OnTouchImageViewListener {
                    override fun onMove() {}
                })
            }
        }

    override fun resetZoom() = binding.ivImage.resetZoom()
}