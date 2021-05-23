package space.taran.arkbrowser.mvp.view.item

import android.content.res.ColorStateList
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_file_grid.view.*
import space.taran.arkbrowser.R
import space.taran.arkbrowser.mvp.model.dao.common.Preview
import space.taran.arkbrowser.utils.ITEMS_CONTAINER
import space.taran.arkbrowser.utils.imageForPredefinedIcon
import space.taran.arkbrowser.utils.loadImage

class FileItemViewHolder(override val containerView: View) :
    RecyclerView.ViewHolder(containerView),
    LayoutContainer, FileItemView {

    override fun position(): Int = this.layoutPosition

    override fun setIcon(icon: Preview): Unit = with(containerView) {
        Log.d(ITEMS_CONTAINER, "setting icon $icon")
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