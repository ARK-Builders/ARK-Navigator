package space.taran.arknavigator.mvp.view.item

import androidx.recyclerview.widget.RecyclerView
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.ItemFileGridBinding
import space.taran.arknavigator.mvp.model.repo.ExtraInfoTag
import space.taran.arknavigator.mvp.model.repo.ResourceMeta
import space.taran.arknavigator.ui.fragments.preview.PreviewAndThumbnail
import space.taran.arknavigator.utils.*
import space.taran.arknavigator.utils.extensions.makeGone
import space.taran.arknavigator.utils.extensions.textOrGone
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

    override fun setIconOrPreview(path: Path, meta: ResourceMeta): Unit = with(binding.root) {
        val placeholder = ImageUtils.iconForExtension(extension(path))
        val thumbnail = PreviewAndThumbnail.locate(path, meta)?.thumbnail

        ImageUtils.loadImageWithPlaceholder(thumbnail, placeholder, binding.iv)

        //todo make it more generic
        if (meta.extra != null) {
            binding.resolutionTV.textOrGone(meta.extra.data[ExtraInfoTag.MEDIA_RESOLUTION])
            binding.durationTV.textOrGone(meta.extra.data[ExtraInfoTag.MEDIA_DURATION])
        } else {
            binding.resolutionTV.makeGone()
            binding.durationTV.makeGone()
        }
    }

    override fun setText(title: String) = with(binding.root) {
        binding.tvTitle.text = title
    }
}