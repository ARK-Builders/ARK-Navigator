package space.taran.arknavigator.utils

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.taran.arknavigator.R
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.utils.extensions.autoDisposeScope
import java.nio.file.Path

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

    fun loadZoomImageWithPlaceholder(image: Path?, placeHolder: Int, view: ImageView) {
        view.setImageResource(placeHolder)
        view.autoDisposeScope.launch {
            withContext(Dispatchers.Main) {
                Glide.with(view.context)
                    .load(image?.toFile())
                    .placeholder(placeHolder)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(
                            resource: Drawable,
                            transition: Transition<in Drawable?>?
                        ) {
                            view.setImageDrawable(resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                        }
                    })
            }
        }
    }

    fun loadImageWithPlaceholder(image: Path?, placeHolder: Int, view: ImageView) {
        view.setImageResource(placeHolder)
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
}
