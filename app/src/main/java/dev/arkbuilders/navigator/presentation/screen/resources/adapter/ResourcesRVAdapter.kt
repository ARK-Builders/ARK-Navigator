package dev.arkbuilders.navigator.presentation.screen.resources.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.arkbuilders.navigator.databinding.ItemFileGridBinding
import dev.arkbuilders.navigator.presentation.App

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
            if (presenter.selectingEnabled) {
                presenter.onSelectedItemLongClick(holder)
            } else {
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
