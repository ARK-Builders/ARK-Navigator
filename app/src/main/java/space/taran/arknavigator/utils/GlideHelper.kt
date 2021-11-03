package space.taran.arknavigator.utils

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.ortiz.touchview.TouchImageView
import space.taran.arknavigator.R
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.ui.activity.MainActivity
import java.nio.file.Path

fun iconForExtension(ext: String): Int {
    val drawableID = App.instance.resources
        .getIdentifier(
            "ic_file_${ext}",
            "drawable",
            App.instance.packageName)

    return if (drawableID > 0) drawableID
    else R.drawable.ic_file
}

fun loadImage(file: Path, container: ImageView) =
    Glide.with(container.context)
        .load(file.toFile())
        .into(container)

fun loadResource(resourceId: Int, container: ImageView) =
    Glide.with(container.context)
        .load(resourceId)
        .into(container)

fun loadImageWithTransition(file: Path?, container: ImageView) =
    Glide.with(container.context)
        .load(file?.toFile())
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(container)

fun loadCroppedImage(file: Path?, container: ImageView) =
    Glide.with(container.context)
        .load(file?.toFile())
        .transition(DrawableTransitionOptions.withCrossFade())
        .centerCrop()
        .into(container)

fun loadImageWithPlaceHolder(file: Path, placeHolder: Int, container: ImageView) =
    Glide.with(container.context)
        .load(file.toFile())
        .placeholder(placeHolder)
        .into(container)

fun loadCroppedImageWithPlaceHolder(file: Path, placeHolder: Int, container: ImageView) =
    Glide.with(container.context)
        .load(file.toFile())
        .placeholder(placeHolder)
        .centerCrop()
        .into(container)

fun loadGifThumbnail(file: Path?, container: ImageView) =
    Glide.with(container.context)
        .asBitmap()
        .load(file?.toFile())
        .into(container)

fun loadGifThumbnailWithPlaceHolder(file: Path?, placeHolder: Int, container: ImageView) =
    Glide.with(container.context)
        .asBitmap()
        .placeholder(placeHolder)
        .load(file?.toFile())
        .into(container)

fun loadBitmap(bitmap: Bitmap?, container: ImageView) {
    Glide.with(container.context)
        .load(bitmap)
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(container)
}

fun loadCroppedBitmap(bitmap: Bitmap?, container: ImageView) {
    Glide.with(container.context)
        .load(bitmap)
        .transition(DrawableTransitionOptions.withCrossFade())
        .centerCrop()
        .into(container)
}

fun loadGif(file: Path?, container: TouchImageView) =
    Glide.with(container.context)
        .asGif()
        .load(file?.toFile())
        .into(object : CustomTarget<GifDrawable>() {
            override fun onResourceReady(
                resource: GifDrawable,
                transition: Transition<in GifDrawable>?
            ) {
                resource.start()
                container.setImageDrawable(resource)
                container.setZoom(1f)
            }

            override fun onLoadCleared(placeholder: Drawable?) {}

        });

fun loadTouchGif(file: Path?, container: ImageView) =
    Glide.with(container.context)
        .asGif()
        .load(file?.toFile())
        .into(container)

fun loadZoomImage(file: Path?, container: ImageView) =
    Glide.with(container.context)
        .load(file?.toFile())
        .into(object : CustomTarget<Drawable?>() {
            override fun onResourceReady(
                resource: Drawable,
                transition: Transition<in Drawable?>?
            ) {
                container.setImageDrawable(resource)
            }

            override fun onLoadCleared(placeholder: Drawable?) {}
        })

fun loadZoomImagePlaceholder(file: Path, placeHolder: Int, container: ImageView) =
    Glide.with(container.context)
        .load(file.toFile())
        .placeholder(placeHolder)
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(object : CustomTarget<Drawable?>() {
            override fun onResourceReady(
                resource: Drawable,
                transition: Transition<in Drawable?>?
            ) {
                container.setImageDrawable(resource)
            }

            override fun onLoadCleared(placeholder: Drawable?) {}
        })