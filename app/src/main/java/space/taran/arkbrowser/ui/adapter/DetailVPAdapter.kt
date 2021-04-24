package space.taran.arkbrowser.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import space.taran.arkbrowser.R
import space.taran.arkbrowser.mvp.presenter.adapter.IDetailListPresenter
import space.taran.arkbrowser.mvp.view.item.DetailItemView
import space.taran.arkbrowser.utils.loadImage
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_image.view.*
import java.io.File

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

        override fun setImage(file: File): Unit = with(containerView) {
            loadImage(file, iv_image)
        }
    }
}