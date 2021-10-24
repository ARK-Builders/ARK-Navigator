package space.taran.arknavigator.mvp.view.item

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import space.taran.arknavigator.databinding.ItemFileGridBinding
import space.taran.arknavigator.mvp.model.repo.PreviewsRepo
import space.taran.arknavigator.mvp.model.repo.ResourceMeta
import space.taran.arknavigator.ui.fragments.utils.Preview
import space.taran.arknavigator.utils.*
import space.taran.arknavigator.utils.extensions.textOrGone
import javax.inject.Inject

class FileItemViewHolder(private val binding: ItemFileGridBinding) :
    RecyclerView.ViewHolder(binding.root),
    FileItemView {

    @Inject
    lateinit var previewsRepo: PreviewsRepo

    override fun position(): Int = this.layoutPosition

    override fun setIcon(icon: Preview, meta: ResourceMeta?): Unit = with(binding.root) {
        Log.d(ITEMS_CONTAINER, "setting icon $icon")

        if (icon.isFolder != null) {
            binding.iv.setImageResource(imageForPredefinedIcon(icon.isFolder))
        } else previewsRepo.loadPreview(binding.iv, icon, meta?.extra, true)

        binding.resolutionTV.textOrGone(previewsRepo.loadExtraMeta(
            extraMeta = meta?.extra,
            filePath = icon.filePath,
            infoTag = Preview.ExtraInfoTag.MEDIA_RESOLUTION
        ))

        binding.durationTV.textOrGone(previewsRepo.loadExtraMeta(
            extraMeta = meta?.extra,
            filePath = icon.filePath,
            infoTag = Preview.ExtraInfoTag.MEDIA_DURATION
        ))
    }

    override fun setText(title: String) = with(binding.root) {
        binding.tvTitle.text = title
    }
}