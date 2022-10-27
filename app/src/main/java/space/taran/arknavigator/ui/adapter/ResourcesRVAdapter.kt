package space.taran.arknavigator.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import space.taran.arknavigator.databinding.ItemFileGridBinding
import space.taran.arknavigator.mvp.presenter.adapter.ResourcesGridPresenter
import space.taran.arknavigator.mvp.view.item.FileItemViewHolder
import space.taran.arknavigator.ui.App

class ResourcesRVAdapter(
    private val presenter: ResourcesGridPresenter
) : RecyclerView.Adapter<FileItemViewHolder>() {
    private var viewHolders = mutableListOf<FileItemViewHolder>()

    override fun getItemCount() = presenter.getCount()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        FileItemViewHolder(
            ItemFileGridBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        ).also {
            App.instance.appComponent.inject(it)
        }

    override fun onBindViewHolder(holder: FileItemViewHolder, position: Int) {
        presenter.bindView(holder)

        holder.binding.root.setOnClickListener {
            if (presenter.selectingEnabled) {
                presenter.onItemSelectChanged(holder)
            } else
                presenter.onItemClick(position)
        }
        holder.binding.root.setOnLongClickListener {
            if (!presenter.selectingEnabled) {
                presenter.onSelectingChanged(true)
                holder.binding.root.performClick()
            }
            return@setOnLongClickListener true
        }
        viewHolders.add(holder)
    }

    override fun onViewRecycled(holder: FileItemViewHolder) {
        super.onViewRecycled(holder)
        viewHolders.remove(holder)
    }

    fun onSelectingChanged(enabled: Boolean) {
        viewHolders.forEach {
            it.onSelectingChanged(enabled)
        }
    }
}
