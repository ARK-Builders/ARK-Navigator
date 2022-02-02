package space.taran.arknavigator.utils

import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.signature.ObjectKey
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.ortiz.touchview.TouchImageView
import space.taran.arknavigator.R
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.ui.App
import java.nio.file.Path

object ImageUtils {
    private const val MAX_GLIDE_SIZE = 1500
    private const val PREVIEW_SIGNATURE = "preview"
    private const val THUMBNAIL_SIGNATURE = "thumbnail"
    const val APPEARANCE_DURATION = 300L

    fun iconForExtension(ext: String): Int {
        val drawableID = App.instance.resources
            .getIdentifier(
                "ic_file_$ext",
                "drawable",
                App.instance.packageName
            )

        return if (drawableID > 0) drawableID
        else R.drawable.ic_file
    }

    fun loadGlideZoomImage(resource: ResourceId, image: Path, view: TouchImageView) =
        Glide.with(view.context)
            .load(image.toFile())
            .apply(
                RequestOptions()
                    .priority(Priority.IMMEDIATE)
                    .signature(ObjectKey("$resource$PREVIEW_SIGNATURE"))
                    .downsample(DownsampleStrategy.CENTER_INSIDE)
                    .override(MAX_GLIDE_SIZE)
            )
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    view.setImageDrawable(resource)
                    view.animate().apply {
                        duration = APPEARANCE_DURATION
                        alpha(1f)
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })

    fun loadSubsamplingImage(image: Path, view: SubsamplingScaleImageView) {
        view.orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF
        view.setImage(ImageSource.uri(image.toString()))
    }

    fun loadThumbnailWithPlaceholder(
        resource: ResourceId,
        image: Path?,
        placeHolder: Int,
        view: ImageView
    ) {
        Log.d(IMAGES, "loading image $image")

        Glide.with(view.context)
            .load(image?.toFile())
            .placeholder(placeHolder)
            .signature(ObjectKey("$resource$THUMBNAIL_SIGNATURE"))
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(view)
    }

    fun <T> glideExceptionListener() = object : RequestListener<T> {
        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<T>?,
            isFirstResource: Boolean
        ): Boolean {
            Log.d(
                GLIDE,
                "load failed with message: ${
                e?.message
                } for target of type: ${
                target?.javaClass?.canonicalName
                }"
            )
            return true
        }

        override fun onResourceReady(
            resource: T,
            model: Any?,
            target: Target<T>?,
            dataSource: DataSource?,
            isFirstResource: Boolean
        ) = false
    }
}
