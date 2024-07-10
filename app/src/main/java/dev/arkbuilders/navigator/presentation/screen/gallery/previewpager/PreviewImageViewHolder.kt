package dev.arkbuilders.navigator.presentation.screen.gallery.previewpager

import android.annotation.SuppressLint
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.ortiz.touchview.OnTouchImageViewListener
import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.data.meta.Metadata
import dev.arkbuilders.arklib.data.preview.PreviewLocator
import dev.arkbuilders.arklib.data.preview.PreviewStatus
import dev.arkbuilders.arklib.utils.ImageUtils
import dev.arkbuilders.navigator.databinding.ItemImageBinding
import dev.arkbuilders.navigator.presentation.screen.gallery.GalleryPresenter
import dev.arkbuilders.navigator.presentation.utils.makeVisibleAndSetOnClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.presenterScope
import timber.log.Timber

@SuppressLint("ClickableViewAccessibility")
class PreviewImageViewHolder(
    private val binding: ItemImageBinding,
    private val presenter: GalleryPresenter,
    private val gestureDetector: GestureDetectorCompat
) : RecyclerView.ViewHolder(binding.root) {

    private var joinPreviewJob: Job? = null
    private var subsamplingImageLoadFailed = false

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

    var pos = -1

    suspend fun setSource(
        placeholder: Int,
        id: ResourceId,
        meta: Metadata,
        locator: PreviewLocator
    ) = with(binding) {
        joinPreviewJob?.cancel()

        if (meta is Metadata.Video) {
            icPlay.makeVisibleAndSetOnClickListener {
                presenter.onPlayButtonClick()
            }
        } else {
            icPlay.isVisible = false
        }

        if (!locator.isGenerated()) {
            progress.isVisible = true
            Timber.d("join preview generation for $id")
            joinPreviewJob = presenter.presenterScope.launch {
                locator.join()

                if (!isActive) return@launch

                withContext(Dispatchers.Main) {
                    progress.isVisible = false
                    onPreviewReady(placeholder, id, meta, locator)
                }
            }
            return@with
        }

        onPreviewReady(placeholder, id, meta, locator)
    }

    private fun onPreviewReady(
        placeholder: Int,
        id: ResourceId,
        meta: Metadata,
        locator: PreviewLocator
    ) = with(binding) {
        val status = locator.check()
        if (status != PreviewStatus.FULLSCREEN) {
            ivZoom.isZoomEnabled = false
            ivZoom.setImageResource(placeholder)
            return@with
        }

        val path = locator.fullscreen()

        // Loading in parallel limited version of image with cache in ivZoom
        // and full version in ivSubsampling
        // User can wait up to several seconds for full version of heavy image
        ImageUtils.loadImage(id, path, ivZoom, limitSize = true)
        ImageUtils.loadSubsamplingImage(path, ivSubsampling)
    }

    fun reset() = with(binding) {
        progress.isVisible = false
        ivZoom.isVisible = true
        ivZoom.isZoomEnabled = true
        subsamplingImageLoadFailed = false
    }

    fun onRecycled() = with(binding) {
        ivSubsampling.recycle()
    }

    // If full version has not yet been loaded, then listen to the zoom, block it
    // and show progress until it is fully loaded
    private fun setZoomImageEventListener() = with(binding) {
        ivZoom.setOnTouchImageViewListener(object : OnTouchImageViewListener {
            override fun onMove() {
                if (!subsamplingImageLoadFailed && ivZoom.isZoomed) {
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

                // Fallback to ivZoom
                // Now this should only happen for .gif and .svg files
                // Subsampling image can't handle them
                override fun onImageLoadError(e: Exception?) {
                    subsamplingImageLoadFailed = true
                    progress.isVisible = false
                    ivZoom.isZoomEnabled = true
                }

                override fun onPreviewLoadError(e: Exception?) {}

                override fun onImageLoaded() {}

                override fun onTileLoadError(e: Exception?) {}

                override fun onPreviewReleased() {}
            }
        )
    }
}
