package space.taran.arknavigator.utils

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import space.taran.arknavigator.R
import space.taran.arknavigator.ui.fragments.utils.PredefinedIcon
import java.nio.file.Path

fun imageForPredefinedIcon(icon: PredefinedIcon): Int =
    when(icon) {
        PredefinedIcon.FOLDER -> R.drawable.ic_baseline_folder
        PredefinedIcon.PDF -> R.drawable.ic_file_pdf
        PredefinedIcon.GIF -> R.drawable.ic_file_gif
        PredefinedIcon.DOC -> R.drawable.ic_file_doc
        PredefinedIcon.DOCX -> R.drawable.ic_file_docx
        PredefinedIcon.HTML -> R.drawable.ic_file_html
        PredefinedIcon.ODT -> R.drawable.ic_file_odt
        PredefinedIcon.ODS -> R.drawable.ic_file_ods
        PredefinedIcon.XLS -> R.drawable.ic_file_xls
        PredefinedIcon.XLSX -> R.drawable.ic_file_xlsx
        else -> R.drawable.ic_file
    }

fun loadImage(file: Path, container: ImageView) =
    Glide.with(container.context)
        .load(file.toFile())
        .into(container)

fun loadResource(resourceId: Int, container: ImageView) =
    Glide.with(container.context)
        .load(resourceId)
        .into(container)

fun loadCroppedImage(file: Path, container: ImageView) =
    Glide.with(container.context)
        .load(file.toFile())
        .transition(DrawableTransitionOptions.withCrossFade())
        .centerCrop()
        .into(container)

fun loadGifThumbnail(file: Path, container: ImageView) =
    Glide.with(container.context)
        .asBitmap()
        .load(file.toFile())
        .into(container)

fun loadGifThumbnailWithPlaceHolder(file: Path, placeHolder: Int, container: ImageView) =
    Glide.with(container.context)
        .asBitmap()
        .placeholder(placeHolder)
        .load(file.toFile())
        .into(container)

fun loadCroppedBitmap(bitmap: Bitmap, container: ImageView) =
    Glide.with(container.context)
        .load(bitmap)
        .transition(DrawableTransitionOptions.withCrossFade())
        .centerCrop()
        .into(container)

fun loadGif(file: Path, container: ImageView) =
    Glide.with(container.context)
        .asGif()
        .load(file.toFile())
        .into(container)

fun loadZoomImage(file: Path, container: ImageView) =
    Glide.with(container.context)
        .load(file.toFile())
        .into(object : CustomTarget<Drawable?>() {
            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable?>?) {
                container.setImageDrawable(resource)
            }
            override fun onLoadCleared(placeholder: Drawable?) {}
        })