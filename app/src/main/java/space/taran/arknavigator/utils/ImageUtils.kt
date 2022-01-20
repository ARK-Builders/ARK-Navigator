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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.taran.arknavigator.R
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.utils.extensions.autoDisposeScope
import java.nio.file.Path

object ImageUtils {
    private const val GALLERY_MAX_IMAGE_SIZE = 2000

    fun iconForExtension(ext: String): Int {
        val drawableID = App.instance.resources
            .getIdentifier(
                "ic_file_${ext}",
                "drawable",
                App.instance.packageName
            )

        return if (drawableID > 0) drawableID
        else R.drawable.ic_file
    }

    fun loadZoomImageWithPlaceholder(image: Path?, placeHolder: Int, view: ImageView) {
        Log.d(IMAGES, "loading image $image")

        if (image == null) {
            view.setImageResource(placeHolder)
            return
        }

        view.setImageDrawable(null)
        view.alpha = 0f

        val requestOptions = RequestOptions()
            .priority(Priority.IMMEDIATE)
            .downsample(DownsampleStrategy.CENTER_INSIDE)
            .override(GALLERY_MAX_IMAGE_SIZE)

        view.autoDisposeScope.launch {
            withContext(Dispatchers.Main) {
                Glide.with(view)
                    .load(image.toFile())
                    .apply(requestOptions)
                    .into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(
                            resource: Drawable,
                            transition: Transition<in Drawable?>?
                        ) {
                            view.setImageDrawable(resource)

                            view.animate().apply {
                                duration = 300
                                alpha(1f)
                            }
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
            }
        }
    }

    fun loadImageWithPlaceholder(image: Path?, placeHolder: Int, view: ImageView) {
        Log.d(IMAGES, "loading image $image")
        view.autoDisposeScope.launch {
            withContext(Dispatchers.Main) {
                Glide.with(view)
                    .load(image?.toFile())
                    .placeholder(placeHolder)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(view)
            }
        }
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
                "load failed with message: ${e?.message} for target of type: ${target?.javaClass?.canonicalName}"
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