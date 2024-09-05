package dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.arkbuilders.arklib.data.meta.Kind
import dev.arkbuilders.arklib.utils.ImageUtils
import dev.arkbuilders.arklib.utils.extension
import dev.arkbuilders.navigator.databinding.ItemImageBinding
import dev.arkbuilders.navigator.databinding.ItemPreviewPlainTextBinding
import dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain.GalleryItem
import dev.arkbuilders.navigator.presentation.screen.resources.adapter.ResourceDiffUtilCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileReader
import java.nio.file.Path

class PreviewsPagerUplift(
    val lifecycleScope: CoroutineScope,
    val context: Context,
    val viewModel: GalleryUpliftViewModel
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var galleryItems = emptyList<GalleryItem>()

    fun dispatchUpdates(newItems: List<GalleryItem>) {
        if (newItems == galleryItems) {
            return
        }
        val diff = DiffUtil.calculateDiff(
            ResourceDiffUtilCallback(
                galleryItems.map { it.resource.id },
                newItems.map { it.resource.id }
            )
        )
        galleryItems = newItems
        diff.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int {
        return galleryItems.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        if (viewType == Kind.PLAINTEXT.ordinal) {
            PreviewPlainTextViewHolderUplift(
                ItemPreviewPlainTextBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ),
                getGestureDetector()
            )
        } else {
            PreviewImageViewHolderUplift(
                ItemImageBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ),
                viewModel,
                getGestureDetector()
            )
        }

    override fun getItemViewType(position: Int) =
        galleryItems[position].metadata.kind.ordinal

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        lifecycleScope.launch {
            when (holder) {
                is PreviewPlainTextViewHolderUplift -> {
                    holder.pos = position
                    val item = galleryItems[position]
                    val text = readText(item.path)
                    text.onSuccess {
                        holder.setContent(it)
                    }
                }

                is PreviewImageViewHolderUplift -> {
                    holder.reset()
                    holder.pos = position
                    val item = galleryItems[position]
                    val placeholder =
                        ImageUtils.iconForExtension(extension(item.path))
                    holder.setSource(
                        placeholder,
                        item.id(),
                        item.metadata,
                        item.preview
                    )
                }
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is PreviewImageViewHolderUplift) {
            holder.onRecycled()
        }
    }

    private fun getGestureDetector(): GestureDetectorCompat {
        val listener = object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                viewModel.onPreviewsItemClick()
                return true
            }
        }
        return GestureDetectorCompat(context, listener)
    }

    private suspend fun readText(source: Path): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val content = FileReader(source.toFile()).readText()
                Result.success(content)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
