package space.taran.arknavigator.utils

import android.widget.ImageView
import com.bumptech.glide.Glide
import space.taran.arknavigator.R
import space.taran.arknavigator.ui.fragments.utils.PredefinedIcon
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