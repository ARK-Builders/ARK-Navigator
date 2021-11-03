package space.taran.arknavigator.mvp.view.item

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.ItemFileGridBinding
import space.taran.arknavigator.mvp.model.repo.ExtraInfoTag
import space.taran.arknavigator.mvp.model.repo.PreviewsRepo
import space.taran.arknavigator.mvp.model.repo.ResourceMeta
import space.taran.arknavigator.ui.fragments.utils.Preview
import space.taran.arknavigator.utils.*
import space.taran.arknavigator.utils.extensions.makeGone
import space.taran.arknavigator.utils.extensions.textOrGone
import java.nio.file.Path
import javax.inject.Inject

class FileItemViewHolder(private val binding: ItemFileGridBinding) :
    RecyclerView.ViewHolder(binding.root),
    FileItemView {

    @Inject
    lateinit var previewsRepo: PreviewsRepo

    override fun position(): Int = this.layoutPosition

    override fun setFolderIcon() =
        binding.iv.setImageResource(R.drawable.ic_baseline_folder)

    override fun setGenericIcon(path: Path) {
        TODO("Not yet implemented")
    }

    override fun setIconOrPreview(path: Path, meta: ResourceMeta): Unit = with(binding.root) {
        Log.d(ITEMS_CONTAINER, "setting icon for ${meta.id}")

        val preview = Preview.provide(path, meta)

        previewsRepo.loadPreview(
            targetView = binding.iv,
            preview = preview,
            extraMeta = meta.extra,
            centerCrop = true)

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