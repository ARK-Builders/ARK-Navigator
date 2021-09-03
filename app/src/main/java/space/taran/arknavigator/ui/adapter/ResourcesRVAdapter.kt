package space.taran.arknavigator.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import space.taran.arknavigator.R
import space.taran.arknavigator.mvp.presenter.adapter.ResourcesGridPresenter
import space.taran.arknavigator.mvp.view.item.FileItemViewHolder

class ResourcesRVAdapter(
    private val presenter: ResourcesGridPresenter
): RecyclerView.Adapter<FileItemViewHolder>() {
    override fun getItemCount() = presenter.getCount()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = FileItemViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.item_file_grid,
            parent,
            false))

    override fun onBindViewHolder(holder: FileItemViewHolder, position: Int) {
        presenter.bindView(holder)

        holder.itemView.setOnClickListener {
            presenter.onItemClick(position)
        }
    }
}