package space.taran.arknavigator.mvp.view.item

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import space.taran.arknavigator.databinding.ItemImageBinding
import space.taran.arknavigator.mvp.model.dao.common.PredefinedIcon
import space.taran.arknavigator.utils.imageForPredefinedIcon
import space.taran.arknavigator.utils.loadImage
import java.nio.file.Path

//todo join with FileItemViewHolder, it is basically the same, just different sizes
class PreviewItemViewHolder(val binding: ItemImageBinding) :
    RecyclerView.ViewHolder(binding.root),
    PreviewItemView {

    override var pos = -1

    override fun setPredefined(resource: PredefinedIcon): Unit = with(binding.root) {
        binding.ivImage.setImageResource(imageForPredefinedIcon(resource))
    }

    override fun setImage(file: Path): Unit = with(binding.root) {
        loadImage(file, binding.ivImage)
    }
}