package space.taran.arkbrowser.utils

import android.widget.ImageView
import com.bumptech.glide.Glide

fun loadImage(url: String, container: ImageView) {
    Glide.with(container.context)
        .load(url)
        .into(container)
}