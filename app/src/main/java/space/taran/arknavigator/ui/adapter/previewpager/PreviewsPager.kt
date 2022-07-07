package space.taran.arknavigator.ui.adapter.previewpager

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import space.taran.arknavigator.databinding.ItemImageBinding
import space.taran.arknavigator.databinding.ItemPreviewPlainTextBinding
import space.taran.arknavigator.mvp.presenter.GalleryItemType
import space.taran.arknavigator.mvp.presenter.GalleryPresenter

class PreviewsPager(val presenter: GalleryPresenter) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemCount() = presenter.resources.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        when (viewType) {
            GalleryItemType.PLAINTEXT.ordinal -> PreviewPlainTextViewHolder(
                ItemPreviewPlainTextBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ),
                presenter
            )
            GalleryItemType.OTHER.ordinal -> PreviewItemViewHolder(
                ItemImageBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ),
                presenter
            )
            else -> error("Wrong viewType")
        }

    override fun getItemViewType(position: Int) =
        presenter.detectItemType(position).ordinal

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
            is PreviewItemViewHolder -> {
                holder.pos = position
                presenter.bindView(holder)
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is PreviewItemViewHolder)
            holder.onRecycled()
    }
}
