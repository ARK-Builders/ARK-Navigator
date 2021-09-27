package space.taran.arknavigator.mvp.view.item

import android.content.res.ColorStateList
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.android.extensions.LayoutContainer
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.ItemFileGridBinding
import space.taran.arknavigator.ui.fragments.utils.PredefinedIcon
import space.taran.arknavigator.ui.fragments.utils.Preview
import space.taran.arknavigator.utils.*
import space.taran.arknavigator.utils.extensions.autoDisposeScope

class FileItemViewHolder(private val binding: ItemFileGridBinding) :
    RecyclerView.ViewHolder(binding.root),
    FileItemView {

    override fun position(): Int = this.layoutPosition

    override fun setIcon(icon: Preview): Unit = with(binding.root) {
        Log.d(ITEMS_CONTAINER, "setting icon $icon")
        if (icon.predefined != null) {
            binding.iv.setImageResource(imageForPredefinedIcon(icon.predefined))
        } else {
            when (icon.fileType) {
                FileType.GIF ->
                    loadGifThumbnailWithPlaceHolder(
                        icon.previewPath!!,
                        imageForPredefinedIcon(PredefinedIcon.GIF),
                        binding.iv)
                FileType.PDF -> {
                    //Temporary workaround for asynchronous loading.
                    //To be changed later on, when indexing will be asynchronous
                    binding.iv.setImageResource(imageForPredefinedIcon(PredefinedIcon.PDF))
                    itemView.autoDisposeScope.launch {
                        withContext(Dispatchers.IO) {
                            if (isPDF(icon.previewPath!!)) {
                                val bitmap =
                                    createPdfPreview(icon.previewPath, binding.root.context)
                                withContext(Dispatchers.Main) {
                                    loadCroppedBitmap(bitmap, binding.iv)
                                }
                            } else withContext(Dispatchers.Main) {
                                loadCroppedImage(icon.previewPath, binding.iv)
                            }
                        }
                    }
                }
                else -> loadCroppedImage(icon.previewPath!!, binding.iv)
            }
        }

        if (icon.extraInfo != null) {
            binding.apply {
                resolutionTV.text = icon.extraInfo[Preview.ExtraInfoTag.MEDIA_RESOLUTION]
                durationTV.text = icon.extraInfo[Preview.ExtraInfoTag.MEDIA_DURATION]

                resolutionTV.visibility =
                    if (icon.extraInfo[Preview.ExtraInfoTag.MEDIA_RESOLUTION] == null) View.GONE
                    else View.VISIBLE

                durationTV.visibility =
                    if (icon.extraInfo[Preview.ExtraInfoTag.MEDIA_DURATION] == null) View.GONE
                    else View.VISIBLE
            }
        } else {
            binding.durationTV.visibility = View.GONE
            binding.resolutionTV.visibility = View.GONE
        }
    }

    override fun setText(title: String) = with(binding.root) {
        binding.tvTitle.text = title
    }
}