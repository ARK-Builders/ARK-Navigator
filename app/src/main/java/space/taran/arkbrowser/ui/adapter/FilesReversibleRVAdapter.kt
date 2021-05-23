package space.taran.arkbrowser.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import space.taran.arkbrowser.R
import space.taran.arkbrowser.mvp.presenter.adapter.ItemsReversiblePresenter
import space.taran.arkbrowser.mvp.view.item.FileItemView
import space.taran.arkbrowser.mvp.view.item.FileItemViewHolder

open class FilesReversibleRVAdapter<Label, Item>(
    private val presenter: ItemsReversiblePresenter<Label, Item, FileItemView>)
    : ItemsReversibleRVAdapter<Label, Item, FileItemView, FileItemViewHolder>(presenter) {

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

}