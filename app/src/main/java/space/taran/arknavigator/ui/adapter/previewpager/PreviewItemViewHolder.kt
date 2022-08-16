package space.taran.arknavigator.ui.adapter.previewpager

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.ortiz.touchview.OnTouchImageViewListener
import space.taran.arknavigator.databinding.ItemImageBinding
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.kind.ResourceKind
import space.taran.arknavigator.mvp.model.repo.preview.PreviewAndThumbnail
import space.taran.arknavigator.mvp.presenter.GalleryPresenter
import space.taran.arknavigator.utils.ImageUtils
import space.taran.arknavigator.utils.ImageUtils.loadGlideZoomImage
import space.taran.arknavigator.utils.ImageUtils.loadSubsamplingImage
import space.taran.arknavigator.utils.extension
import space.taran.arknavigator.utils.extensions.makeVisibleAndSetOnClickListener
import java.nio.file.Path

@SuppressLint("ClickableViewAccessibility")
class PreviewItemViewHolder(
    private val binding: ItemImageBinding,
    private val presenter: GalleryPresenter,
) : RecyclerView.ViewHolder(binding.root), PreviewItemView {

    init {
        val gestureDetector = getGestureDetector()
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
        source: Path,
        meta: ResourceMeta,
        previewAndThumbnail: PreviewAndThumbnail?
    ) = with(binding) {
        val placeholder = ImageUtils.iconForExtension(extension(source))

        if (meta.kind is ResourceKind.Video) {
            icPlay.makeVisibleAndSetOnClickListener {
                presenter.onPlayButtonClick()
            }
        } else {
            icPlay.isVisible = false
        }

        loadImage(meta.id, previewAndThumbnail?.preview, placeholder)
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

    private fun loadImage(id: ResourceId, preview: Path?, placeholder: Int) =
        with(binding) {
            if (preview == null) {
                ivZoom.isZoomEnabled = false
                ivZoom.setImageResource(placeholder)
                return
            }

            loadGlideZoomImage(id, preview, ivZoom)
            loadSubsamplingImage(preview, ivSubsampling)
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

    private fun getGestureDetector(): GestureDetectorCompat {
        val listener = object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                presenter.onPreviewsItemClick()
                return true
            }
        }
        return GestureDetectorCompat(itemView.context, listener)
    }
}
