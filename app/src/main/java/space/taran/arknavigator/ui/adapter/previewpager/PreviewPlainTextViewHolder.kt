package space.taran.arknavigator.ui.adapter.previewpager

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.RecyclerView
import space.taran.arknavigator.databinding.ItemPreviewPlainTextBinding
import space.taran.arknavigator.mvp.presenter.GalleryPresenter

@SuppressLint("ClickableViewAccessibility")
class PreviewPlainTextViewHolder(
    private val binding: ItemPreviewPlainTextBinding,
    private val presenter: GalleryPresenter
) : RecyclerView.ViewHolder(binding.root), PreviewPlainTextItemView {
    override var pos = -1

    init {
        val detector = getGestureDetector()
        binding.tvContent.setOnTouchListener { view, event ->
            return@setOnTouchListener detector.onTouchEvent(event)
        }
    }

    override fun setContent(text: String) = with(binding) {
        tvContent.text = text
        tvContent.animate().apply {
            duration = 300
            alpha(1f)
        }
        return@with
    }

    override fun reset() = with(binding) {
        tvContent.alpha = 0f
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
