package space.taran.arknavigator.mvp.model.repo

import android.widget.ImageView
import com.ortiz.touchview.TouchImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.taran.arknavigator.ui.fragments.utils.Preview
import space.taran.arknavigator.utils.*
import space.taran.arknavigator.utils.extensions.*

class PreviewsRepo {

    fun loadPreview(
        targetView: ImageView,
        preview: Preview,
        extraMeta: ResourceMetaExtra?,
        centerCrop: Boolean = false
    ) {
        when (extraMeta?.type) {
            ResourceType.GIF -> {
                if (targetView is TouchImageView) {
                    loadGif(
                        preview.imagePath,
                        targetView
                    )
                } else {
                    loadGifThumbnailWithPlaceHolder(
                        preview.imagePath,
                        iconForExtension("gif"),
                        targetView
                    )
                }
            }
            ResourceType.PDF -> {
                targetView.setImageResource(iconForExtension("pdf"))
                targetView.autoDisposeScope.launch {
                    withContext(Dispatchers.Main) {
                        if (centerCrop)
                            loadCroppedBitmap(bitmap, targetView)
                        else loadBitmap(bitmap, targetView)
                    }
                }
            } else -> {
                if (fileExtension != null && filePath != null) {
                    if (centerCrop) {
                        loadCroppedImageWithPlaceHolder(
                            preview.imagePath,
                            iconForExtension(fileExtension),
                            targetView
                        )
                    } else loadImageWithPlaceHolder(
                        preview.imagePath,
                        iconForExtension(fileExtension),
                        targetView
                    )
                } else if (fileExtension != null)
                    targetView.setImageResource(iconForExtension(preview.fileExtension))
                else {
                    if (centerCrop) loadCroppedImage(
                        filePath!!,
                        targetView
                    )
                    else loadImageWithTransition(filePath, targetView)
                }
            }
        }
    }
}