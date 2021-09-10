package space.taran.arknavigator.mvp.view.item

import androidx.recyclerview.widget.RecyclerView
import space.taran.arknavigator.databinding.ItemImageBinding
import space.taran.arknavigator.ui.fragments.utils.PredefinedIcon
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