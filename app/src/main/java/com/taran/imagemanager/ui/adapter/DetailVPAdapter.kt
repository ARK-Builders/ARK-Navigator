package com.taran.imagemanager.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.taran.imagemanager.R
import com.taran.imagemanager.mvp.presenter.adapter.IDetailListPresenter
import com.taran.imagemanager.mvp.view.item.DetailItemView
import com.taran.imagemanager.utils.loadImage
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_image.view.*

class DetailVPAdapter(
    val presenter: IDetailListPresenter
): RecyclerView.Adapter<DetailVPAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.item_image,
            parent,
            false
        )
    )

    override fun getItemCount() = presenter.getCount()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.pos = position
        presenter.bindView(holder)
    }

    inner class ViewHolder(override val containerView: View) :
        RecyclerView.ViewHolder(containerView),
        LayoutContainer, DetailItemView {

        override var pos = -1

        override fun setImage(path: String) = with(containerView) {
            loadImage(path, iv_image)
        }

    }

}