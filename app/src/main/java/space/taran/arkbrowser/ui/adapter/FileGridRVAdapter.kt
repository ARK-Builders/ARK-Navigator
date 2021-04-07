package space.taran.arkbrowser.ui.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import space.taran.arkbrowser.R
import space.taran.arkbrowser.mvp.model.entity.common.Icons
import space.taran.arkbrowser.mvp.presenter.adapter.IFileGridPresenter
import space.taran.arkbrowser.mvp.view.item.FileItemView
import space.taran.arkbrowser.utils.loadImage
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
                Icons.FOLDER -> {
                    iv.setImageResource(R.drawable.ic_baseline_folder)
                    iv.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray))
                }
                Icons.PLUS -> {
                    iv.setImageResource(R.drawable.ic_baseline_add)
                    iv.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray))
                }
                Icons.FILE -> {
                    iv.setImageResource(R.drawable.ic_file)
                    iv.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray))
                }
                Icons.ROOT -> {
                    iv.setImageResource(R.drawable.ic_root)
                    iv.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray))
                }
                Icons.IMAGE -> loadImage(path!!, iv)
            }
        }

        override fun setFav() = with(containerView) {
            iv_fav.visibility = View.VISIBLE
        }

        override fun setText(title: String) = with(containerView) {
            tv_title.text = title
        }
    }

}
