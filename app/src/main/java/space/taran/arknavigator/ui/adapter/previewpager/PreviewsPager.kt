package space.taran.arknavigator.ui.adapter.previewpager

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.RecyclerView
import space.taran.arklib.domain.meta.Kind
import space.taran.arknavigator.databinding.ItemImageBinding
import space.taran.arknavigator.databinding.ItemPreviewPlainTextBinding
import space.taran.arknavigator.mvp.presenter.GalleryPresenter

class PreviewsPager(
    val context: Context,
    val presenter: GalleryPresenter
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemCount() = presenter.galleryItems.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        if (viewType == Kind.PLAINTEXT.ordinal) {
            PreviewPlainTextViewHolder(
                ItemPreviewPlainTextBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ),
                getGestureDetector()
            )
        } else {
            PreviewImageViewHolder(
                ItemImageBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ),
                presenter,
                getGestureDetector()
            )
        }

    override fun getItemViewType(position: Int) =
        presenter.getKind(position).ordinal

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        when (holder) {
            is PreviewPlainTextViewHolder -> {
                holder.pos = position
                presenter.bindPlainTextView(holder)
            }
            is PreviewImageViewHolder -> {
                holder.pos = position
                presenter.bindView(holder)
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is PreviewImageViewHolder)
            holder.onRecycled()
    }

    private fun getGestureDetector(): GestureDetectorCompat {
        val listener = object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                presenter.onPreviewsItemClick()
                return true
            }
        }
        return GestureDetectorCompat(context, listener)
    }
}
