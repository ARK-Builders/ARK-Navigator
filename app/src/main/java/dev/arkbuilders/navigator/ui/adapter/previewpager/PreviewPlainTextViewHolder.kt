package dev.arkbuilders.navigator.ui.adapter.previewpager

import android.annotation.SuppressLint
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.RecyclerView
import dev.arkbuilders.navigator.databinding.ItemPreviewPlainTextBinding

@SuppressLint("ClickableViewAccessibility")
class PreviewPlainTextViewHolder(
    private val binding: ItemPreviewPlainTextBinding,
    private val detector: GestureDetectorCompat
) : RecyclerView.ViewHolder(binding.root), PreviewPlainTextItemView {
    override var pos = -1

    init {
        binding.tvContent.setOnTouchListener { view, event ->
            return@setOnTouchListener detector.onTouchEvent(event)
        }
    }

    override fun setContent(text: String) = with(binding) {
        tvContent.text = text
    }

    override fun reset() = with(binding) {
        tvContent.text = ""
    }
}
