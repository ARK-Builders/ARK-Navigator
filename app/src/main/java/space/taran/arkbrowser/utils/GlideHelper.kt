package space.taran.arkbrowser.utils

import android.widget.ImageView
import com.bumptech.glide.Glide
import space.taran.arkbrowser.R
import space.taran.arkbrowser.mvp.model.entity.common.Icon
import java.nio.file.Path

fun iconToImageResource(icon: Icon): Int =
    when(icon) {
        Icon.FOLDER -> {
            R.drawable.ic_baseline_folder
        }
        Icon.PLUS -> {
            R.drawable.ic_baseline_add
        }
        Icon.FILE -> {
            R.drawable.ic_file
        }
        Icon.ROOT -> {
            R.drawable.ic_root
        }
    }

fun loadImage(file: Path, container: ImageView) =
    Glide.with(container.context)
        .load(file)
        .into(container)