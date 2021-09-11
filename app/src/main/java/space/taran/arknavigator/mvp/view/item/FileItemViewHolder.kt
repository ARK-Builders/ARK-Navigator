package space.taran.arknavigator.mvp.view.item

import android.content.res.ColorStateList
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.ItemFileGridBinding
import space.taran.arknavigator.ui.fragments.utils.Preview
import space.taran.arknavigator.utils.ITEMS_CONTAINER
import space.taran.arknavigator.utils.imageForPredefinedIcon
import space.taran.arknavigator.utils.loadImage

class FileItemViewHolder(private val binding: ItemFileGridBinding) :
    RecyclerView.ViewHolder(binding.root),
    FileItemView {

    override fun position(): Int = this.layoutPosition

    override fun setIcon(icon: Preview): Unit = with(binding.root) {
        Log.d(ITEMS_CONTAINER, "setting icon $icon")
        if (icon.predefined != null) {
            binding.iv.setImageResource(imageForPredefinedIcon(icon.predefined))
            binding.iv.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray))
        } else {
            loadImage(icon.image!!, binding.iv)
        }
    }

    override fun setText(title: String) = with(binding.root) {
        binding.tvTitle.text = title
    }
}