package space.taran.arknavigator.mvp.view.item

import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.taran.arknavigator.databinding.ItemFileGridBinding
import space.taran.arknavigator.ui.fragments.utils.Preview
import space.taran.arknavigator.utils.*
import space.taran.arknavigator.utils.extensions.autoDisposeScope

class FileItemViewHolder(private val binding: ItemFileGridBinding) :
    RecyclerView.ViewHolder(binding.root),
    FileItemView {

    override fun position(): Int = this.layoutPosition

    override fun setIcon(icon: Preview): Unit = with(binding.root) {
        Log.d(ITEMS_CONTAINER, "setting icon $icon")
        if (icon.predefinedIcon != null) {
            binding.iv.setImageResource(imageForPredefinedIcon(icon.predefinedIcon))
        } else {
            when (icon.fileType) {
                FileType.GIF -> loadGifThumbnailWithPlaceHolder(
                    icon.previewPath!!,
                    imageForPredefinedExtension("gif"),
                    binding.iv)
                FileType.PDF -> {
                    //Temporary workaround for asynchronous loading.
                    //To be changed later on, when indexing will be asynchronous
                    binding.iv.setImageResource(imageForPredefinedExtension("pdf"))
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
                else -> {
                    if (icon.fileExtension != null && icon.previewPath != null) {
                        loadCroppedImageWithPlaceHolder(
                            icon.previewPath,
                            imageForPredefinedExtension(icon.fileExtension),
                            binding.iv
                        )
                    } else if (icon.fileExtension != null)
                        binding.iv.setImageResource(imageForPredefinedExtension(icon.fileExtension))
                    else loadCroppedImage(
                        icon.previewPath!!,
                        binding.iv
                    )
                }
            }
        }

        if (icon.extraInfo != null) {
            val extraInfo = icon.extraInfo!!
            binding.apply {
                resolutionTV.text = extraInfo[Preview.ExtraInfoTag.MEDIA_RESOLUTION]
                durationTV.text = extraInfo[Preview.ExtraInfoTag.MEDIA_DURATION]

                resolutionTV.visibility =
                    if (extraInfo[Preview.ExtraInfoTag.MEDIA_RESOLUTION] == null) View.GONE
                    else View.VISIBLE

                durationTV.visibility =
                    if (extraInfo[Preview.ExtraInfoTag.MEDIA_DURATION] == null) View.GONE
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