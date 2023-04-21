package space.taran.arknavigator.ui.adapter.previewpager

import android.annotation.SuppressLint
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.ortiz.touchview.OnTouchImageViewListener
import space.taran.arknavigator.databinding.ItemImageBinding
import space.taran.arklib.ResourceId
import space.taran.arklib.domain.meta.Metadata
import space.taran.arklib.domain.preview.PreviewLocator
import space.taran.arklib.domain.preview.PreviewStatus
import space.taran.arknavigator.mvp.presenter.GalleryPresenter
import space.taran.arklib.utils.ImageUtils.loadGlideZoomImage
import space.taran.arklib.utils.ImageUtils.loadSubsamplingImage
import space.taran.arknavigator.utils.extensions.makeVisibleAndSetOnClickListener

@SuppressLint("ClickableViewAccessibility")
class PreviewImageViewHolder(
    private val binding: ItemImageBinding,
    private val presenter: GalleryPresenter,
    private val gestureDetector: GestureDetectorCompat
) : RecyclerView.ViewHolder(binding.root), PreviewItemView {

    init {
        binding.ivSubsampling.setOnTouchListener { view, motionEvent ->
            return@setOnTouchListener gestureDetector.onTouchEvent(motionEvent)
        }
        binding.ivZoom.setOnTouchListener { view, motionEvent ->
            return@setOnTouchListener gestureDetector.onTouchEvent(motionEvent)
        }
        setZoomImageEventListener()
        setSubsamplingEventListener()
    }

    override var pos = -1

    override fun setSource(
        placeholder: Int,
        id: ResourceId,
        meta: Metadata,
        preview: PreviewLocator
    ) = with(binding) {
        if (meta is Metadata.Video) {
            icPlay.makeVisibleAndSetOnClickListener {
                presenter.onPlayButtonClick()
            }
        } else {
            icPlay.isVisible = false
        }

        val status = preview.check()
        if (status != PreviewStatus.FULLSCREEN) {
            ivZoom.isZoomEnabled = false
            ivZoom.setImageResource(placeholder)
            return@with
        }

        val path = preview.fullscreen()
        loadGlideZoomImage(id, path, ivZoom)
        loadSubsamplingImage(path, ivSubsampling)
    }

    override fun reset() = with(binding) {
        progress.isVisible = false
        ivZoom.isVisible = true
        ivZoom.isZoomEnabled = true
    }

    fun onRecycled() = with(binding) {
        ivSubsampling.recycle()
        Glide.with(ivZoom.context).clear(ivZoom)
    }

    private fun setZoomImageEventListener() = with(binding) {
        ivZoom.setOnTouchImageViewListener(object : OnTouchImageViewListener {
            override fun onMove() {
                if (ivZoom.isZoomed) {
                    progress.isVisible = true
                    ivZoom.isZoomEnabled = false
                    ivZoom.resetZoom()
                }
            }
        })
    }

    private fun setSubsamplingEventListener() = with(binding) {
        ivSubsampling.setOnImageEventListener(
            object : SubsamplingScaleImageView.OnImageEventListener {
                override fun onReady() {
                    ivZoom.isVisible = false
                    progress.isVisible = false
                    ivZoom.setImageDrawable(null)
                }

                override fun onImageLoadError(e: Exception?) {}

                override fun onPreviewLoadError(e: Exception?) {}

                override fun onImageLoaded() {}

                override fun onTileLoadError(e: Exception?) {}

                override fun onPreviewReleased() {}
            })
    }
}
