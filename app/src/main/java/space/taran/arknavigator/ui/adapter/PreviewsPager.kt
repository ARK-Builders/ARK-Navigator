package space.taran.arknavigator.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import space.taran.arknavigator.databinding.ItemImageBinding
import space.taran.arknavigator.mvp.presenter.GalleryPresenter
import space.taran.arknavigator.mvp.view.item.PreviewItemViewHolder
import space.taran.arknavigator.ui.App

class PreviewsPager(val presenter: GalleryPresenter) :
    RecyclerView.Adapter<PreviewItemViewHolder>() {

    override fun getItemCount() = presenter.resources.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        PreviewItemViewHolder(
            ItemImageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            presenter
        ).also {
            App.instance.appComponent.inject(it)
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(
        holder: PreviewItemViewHolder,
        position: Int
    ) {
        holder.pos = position
        presenter.bindView(holder)
    }

    override fun onViewRecycled(holder: PreviewItemViewHolder) {
        super.onViewRecycled(holder)
        with(holder.binding) {
            ivSubsampling.recycle()
            Glide.with(ivZoom.context).clear(ivZoom)
        }
    }
}
