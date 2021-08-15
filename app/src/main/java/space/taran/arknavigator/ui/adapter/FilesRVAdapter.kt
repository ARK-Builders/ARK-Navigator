package space.taran.arknavigator.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import space.taran.arknavigator.R
import space.taran.arknavigator.mvp.presenter.adapter.ItemsClickablePresenter
import space.taran.arknavigator.mvp.view.item.FileItemView
import space.taran.arknavigator.mvp.view.item.FileItemViewHolder
import space.taran.arknavigator.utils.ITEMS_CONTAINER

open class FilesRVAdapter<Item>(
    private val presenter: ItemsClickablePresenter<Item, FileItemView>)
    : RecyclerView.Adapter<FileItemViewHolder>() {

    override fun getItemCount() = presenter.getCount()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        FileItemViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_file_grid,
                parent,
                false))

    override fun onBindViewHolder(holder: FileItemViewHolder, position: Int) {
        presenter.bindView(holder)

        holder.itemView.setOnClickListener {
            presenter.itemClicked(position)
        }
    }

    //this is fine when items come from user
    fun updateItems(items: List<Item>) {
        Log.d(ITEMS_CONTAINER, "update requested")
        presenter.updateItems(items)
        this.notifyDataSetChanged()
    }
}
