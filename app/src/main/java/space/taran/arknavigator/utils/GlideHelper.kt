package space.taran.arknavigator.utils

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import space.taran.arknavigator.R
import space.taran.arknavigator.mvp.model.dao.common.PredefinedIcon
import java.nio.file.Path

fun imageForPredefinedIcon(icon: PredefinedIcon): Int =
    when(icon) {
        PredefinedIcon.FOLDER -> {
            R.drawable.ic_baseline_folder
        }
        PredefinedIcon.FILE -> {
            R.drawable.ic_file
        }
    }

fun loadImage(file: Path, container: ImageView) =
    Glide.with(container.context)
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