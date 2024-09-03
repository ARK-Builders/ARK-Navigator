package dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift

import android.annotation.SuppressLint
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.RecyclerView
import dev.arkbuilders.navigator.databinding.ItemPreviewPlainTextBinding

@SuppressLint("ClickableViewAccessibility")
class PreviewPlainTextViewHolderUplift(
    private val binding: ItemPreviewPlainTextBinding,
    private val detector: GestureDetectorCompat
) : RecyclerView.ViewHolder(binding.root) {
    var pos = -1

    init {
        binding.tvContent.setOnTouchListener { view, event ->
            return@setOnTouchListener detector.onTouchEvent(event)
        }
    }

    fun setContent(text: String) = with(binding) {
        tvContent.text = text
    }
}
