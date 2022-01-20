package space.taran.arknavigator.mvp.view.item

import androidx.recyclerview.widget.RecyclerView
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.ItemFileGridBinding
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.preview.PreviewAndThumbnail
import space.taran.arknavigator.ui.extra.ExtraLoader
import space.taran.arknavigator.utils.ImageUtils
import space.taran.arknavigator.utils.extension
import java.nio.file.Path

class FileItemViewHolder(val binding: ItemFileGridBinding) :
    RecyclerView.ViewHolder(binding.root),
    FileItemView {

    override fun position(): Int = this.layoutPosition

    override fun setFolderIcon() =
        binding.iv.setImageResource(R.drawable.ic_baseline_folder)

    override fun setGenericIcon(path: Path) {
        val placeholder = ImageUtils.iconForExtension(extension(path))
        binding.iv.setImageResource(placeholder)
    }

    override fun setIconOrPreview(path: Path, resource: ResourceMeta): Unit = with(binding.root) {
        val placeholder = ImageUtils.iconForExtension(extension(path))
        val thumbnail = PreviewAndThumbnail.locate(path, resource)?.thumbnail

        ImageUtils.loadImageWithPlaceholder(thumbnail, placeholder, binding.iv)
        ExtraLoader.load(resource, listOf(binding.primaryExtra, binding.secondaryExtra),  verbose = false)
    }

    override fun setText(title: String) = with(binding.root) {
        binding.tvTitle.text = title
    }
}