package space.taran.arknavigator.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import space.taran.arknavigator.databinding.ItemFileGridBinding
import space.taran.arknavigator.mvp.presenter.adapter.ItemsReversiblePresenter
import space.taran.arknavigator.mvp.view.item.FileItemView
import space.taran.arknavigator.mvp.view.item.FileItemViewHolder
import space.taran.arknavigator.ui.App

open class FilesReversibleRVAdapter<Label, Item>(
    private val presenter: ItemsReversiblePresenter<Label, Item, FileItemView>
) : ItemsReversibleRVAdapter<Label, Item, FileItemView, FileItemViewHolder>(
    presenter
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        FileItemViewHolder(
            ItemFileGridBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
            .also {
                App.instance.appComponent.inject(it)
            }

    override fun onBindViewHolder(holder: FileItemViewHolder, position: Int) {
        presenter.bindView(holder)

        holder.itemView.setOnClickListener {
            presenter.itemClicked(position)
        }
    }

    override fun onViewRecycled(holder: FileItemViewHolder) {
        super.onViewRecycled(holder)
        with(holder.binding) {
            Glide.with(iv.context).clear(iv)
        }
    }
}
