package space.taran.arknavigator.mvp.view.item

import androidx.recyclerview.widget.RecyclerView
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.ItemFileGridBinding
import space.taran.arknavigator.mvp.model.repo.index.ResourceMetaExtra
import space.taran.arknavigator.mvp.model.repo.preview.Preview
import space.taran.arknavigator.utils.ImageUtils
import space.taran.arknavigator.utils.extension
import java.nio.file.Path

class FileItemViewHolder(private val binding: ItemFileGridBinding) :
    RecyclerView.ViewHolder(binding.root),
    FileItemView {

    override fun position(): Int = this.layoutPosition

    override fun setFolderIcon() =
        binding.iv.setImageResource(R.drawable.ic_baseline_folder)

    override fun setGenericIcon(path: Path) {
        val placeholder = ImageUtils.iconForExtension(extension(path))
        binding.iv.setImageResource(placeholder)
    }

    override fun setIconOrPreview(path: Path, preview: Preview): Unit = with(binding.root) {
        val placeholder = ImageUtils.iconForExtension(extension(path))

        ImageUtils.loadImageWithPlaceholder(preview.thumbnail, placeholder, binding.iv)

        ResourceMetaExtra.draw(
            preview.meta.kind,
            preview.meta.extra,
            arrayOf(binding.primaryExtra, binding.secondaryExtra),
            verbose = false)
    }

    override fun setText(title: String) = with(binding.root) {
        binding.tvTitle.text = title
    }
}