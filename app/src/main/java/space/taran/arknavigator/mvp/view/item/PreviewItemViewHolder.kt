package space.taran.arknavigator.mvp.view.item

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import space.taran.arknavigator.databinding.ItemImageBinding
import space.taran.arknavigator.mvp.model.repo.index.ResourceKind
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.presenter.adapter.PreviewsPagerPresenter
import space.taran.arknavigator.utils.extensions.autoDisposeScope
import space.taran.arknavigator.utils.extensions.makeVisibleAndSetOnClickListener

class PreviewItemViewHolder(
    val binding: ItemImageBinding,
    val presenter: PreviewsPagerPresenter
) :
    RecyclerView.ViewHolder(binding.root),
    PreviewItemView {

    private var preview: Path? = null

    override var pos = -1

    override fun setSource(preview: Path?, placeholder: Int, resource: ResourceMeta) = with(binding) {
        this@PreviewItemViewHolder.preview = preview
        layoutProgress.root.isVisible = false

        if (resource.kind == ResourceKind.VIDEO) {
            icPlay.makeVisibleAndSetOnClickListener {
                presenter.onPlayButtonClick(pos)
            }
        } else {
            icPlay.isVisible = false
        }

        ivSubsampling.isZoomEnabled =
            resource.kind == ResourceKind.IMAGE || resource.kind == ResourceKind.DOCUMENT

        loadImage(preview, placeholder)
    }

    override fun onItemSelected() {
        showProgressIfImageNotReady()
        resetZoom()
    }

    private fun showProgressIfImageNotReady(): Unit = with(binding) {
        ivSubsampling.autoDisposeScope.launch {
            delay(WAIT_LOADING_MILLIS)
            if (preview != null && !ivSubsampling.isReady)
                progress.alpha = 1f
        }
    }

    private fun resetZoom() = with(binding.ivSubsampling) {
        animateScaleAndCenter(0f, center)?.start()
    }

    private fun loadImage(preview: Path?, placeholder: Int) = with(binding) {
        ivSubsampling.alpha = 0f
        progress.alpha = 0f
        ivPlaceholder.alpha = 0f

        if (preview == null) {
            ivPlaceholder.alpha = 1f
            ivPlaceholder.setImageResource(placeholder)
            return
        }

        setSubSamplingEventListener()
        ivSubsampling.orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF
        ivSubsampling.setImage(ImageSource.uri(preview.toString()))
    }

    private fun setSubSamplingEventListener() = with(binding) {
        ivSubsampling.setOnImageEventListener(object :
            SubsamplingScaleImageView.OnImageEventListener {
            override fun onReady() {
                progress.animate().apply {
                    duration = 200
                    alpha(0f)
                }
                ivSubsampling.animate().apply {
                    duration = 400
                    alpha(1f)
                }
            }

            override fun onImageLoadError(e: Exception?) {
                progress.animate().apply {
                    duration = 200
                    alpha(0f)
                }
            }

            override fun onPreviewLoadError(e: Exception?) {}

            override fun onImageLoaded() {}

            override fun onTileLoadError(e: Exception?) {}

            override fun onPreviewReleased() {}
        })
    }

    companion object {
        private const val WAIT_LOADING_MILLIS = 500L
    }
}
