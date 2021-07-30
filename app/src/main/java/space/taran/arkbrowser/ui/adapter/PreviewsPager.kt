package space.taran.arkbrowser.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import space.taran.arkbrowser.R
import space.taran.arkbrowser.mvp.presenter.adapter.PreviewsList
import space.taran.arkbrowser.mvp.view.item.PreviewItemViewHolder

class PreviewsPager(val presenter: PreviewsList)
    : RecyclerView.Adapter<PreviewItemViewHolder>() {

    override fun getItemCount() = presenter.getCount()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        PreviewItemViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_image,
                parent,
                false))

    override fun onBindViewHolder(holder: PreviewItemViewHolder, position: Int) {
        holder.pos = position
        presenter.bindView(holder)
        holder.setOnClickHandler {
            presenter.itemClicked(position)
        }
    }

    fun removeItem(position: Int) {
        val items = presenter.items().toMutableList()
        items.removeAt(position)
        presenter.updateItems(items.toList())
        super.notifyItemRemoved(position)
    }
}