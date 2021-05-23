package space.taran.arkbrowser.utils

import android.widget.ImageView
import com.bumptech.glide.Glide
import space.taran.arkbrowser.R
import space.taran.arkbrowser.mvp.model.dao.common.PredefinedIcon
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