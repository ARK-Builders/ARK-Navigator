package space.taran.arknavigator.utils

import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.taran.arknavigator.R
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.utils.extensions.autoDisposeScope

object ImageUtils {
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
