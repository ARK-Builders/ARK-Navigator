package space.taran.arkbrowser.ui.adapter

import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import space.taran.arkbrowser.R
import space.taran.arkbrowser.mvp.presenter.adapter.ItemGridPresenter
import space.taran.arkbrowser.mvp.view.item.FileItemView
import space.taran.arkbrowser.utils.loadImage
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_file_grid.view.*
import space.taran.arkbrowser.mvp.model.entity.common.Icon
import space.taran.arkbrowser.utils.ITEM_GRID
import space.taran.arkbrowser.utils.RESOURCES_SCREEN
import space.taran.arkbrowser.utils.imageForPredefinedIcon

open class ItemGridRVAdapter<L, I>(val presenter: ItemGridPresenter<L, I>)
    : RecyclerView.Adapter<ItemGridRVAdapter<L, I>.ViewHolder>() {

    override fun getItemCount() = presenter.getCount()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_file_grid,
                parent,
                false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        presenter.bindView(holder)

        holder.itemView.setOnClickListener {
            presenter.itemClicked(position)
        }
    }

    fun updateItems(label: L, items: List<I>) {
        Log.d(ITEM_GRID, "update requested")
        presenter.updateItems(label, items)
        this.notifyDataSetChanged()
    }

    open fun backClicked(): L? {
        Log.d(ITEM_GRID, "back clicked")
        val label = presenter.backClicked()
        if (label != null) {
            this.notifyDataSetChanged()
        }
        return label
    }

    fun getLabel() = presenter.label()

    inner class ViewHolder(override val containerView: View) :
        RecyclerView.ViewHolder(containerView),
        LayoutContainer, FileItemView {

        override fun position(): Int = this.layoutPosition

        override fun setIcon(icon: Icon): Unit = with(containerView) {
            Log.d(ITEM_GRID, "setting icon $icon")
            if (icon.predefined != null) {
                iv.setImageResource(imageForPredefinedIcon(icon.predefined))
                iv.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray))
            } else {
                loadImage(icon.image!!, iv)
            }
        }

        override fun setText(title: String) = with(containerView) {
            tv_title.text = title
        }
    }
}