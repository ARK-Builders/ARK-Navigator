package com.taran.imagemanager.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.taran.imagemanager.R
import com.taran.imagemanager.mvp.model.entity.Icons
import com.taran.imagemanager.mvp.presenter.adapter.IFileGridPresenter
import com.taran.imagemanager.mvp.view.item.FileItemView
import com.taran.imagemanager.utils.loadImage
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_file_grid.view.*

class FileGridRVAdapter(
    val presenter: IFileGridPresenter
): RecyclerView.Adapter<FileGridRVAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_file_grid,
                parent,
                false
            )
        )

    override fun getItemCount() = presenter.getCount()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.pos = position
        presenter.bindView(holder)

        holder.itemView.setOnClickListener {
            presenter.onCardClicked(position)
        }
    }

    inner class ViewHolder(override val containerView: View) :
        RecyclerView.ViewHolder(containerView),
        LayoutContainer, FileItemView {

        override var pos = -1

        override fun setIcon(resourceType: Icons, path: String?) = with(containerView) {
            when(resourceType) {
                Icons.FOLDER -> iv.setImageResource(R.drawable.ic_baseline_folder)
                Icons.PLUS -> iv.setImageResource(R.drawable.ic_baseline_add)
                Icons.IMAGE -> loadImage(path!!, iv)
            }
        }

        override fun setText(title: String) = with(containerView) {
            tv_title.text = title
        }
    }

}
