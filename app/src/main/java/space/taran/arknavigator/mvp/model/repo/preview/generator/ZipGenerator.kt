package space.taran.arknavigator.mvp.model.repo.preview.generator

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import space.taran.arknavigator.R
import space.taran.arknavigator.ui.App

object ZipGenerator {

    fun generate(): Bitmap {
        val drawable = ContextCompat.getDrawable(
            App.instance,
            R.drawable.ic_file_zip
        )

        val bitmap = Bitmap.createBitmap(
            drawable!!.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }
}
