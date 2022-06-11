package space.taran.arknavigator.mvp.model.repo.preview.generator

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.FileReader
import java.nio.file.Path
import space.taran.arknavigator.ui.App

object TxtPreviewGenerator {
    // it is padding in preview image
    val padding = 10f * App.instance.resources.displayMetrics.density
    fun generate(source: Path): Bitmap {
        val fr = FileReader(source.toFile())
        val textPaint = TextPaint()
        textPaint.isAntiAlias = true
        textPaint.textSize =
            10 * App.instance.resources.displayMetrics.density
        textPaint.color = -0x1000000

        val numberOfLines = fr.readLines()
        val text = if (numberOfLines.size > 10)
            numberOfLines.subList(0, 10).joinToString("\n")
        else numberOfLines.subList(0, numberOfLines.size).joinToString("\n")
        val width = (220 * App.instance.resources.displayMetrics.density).toInt()
        val staticLayout = StaticLayout.Builder.obtain(
            text,
            0,
            text.length,
            textPaint,
            width
        ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(5f, 0.9f)
            .setIncludePad(true)
            .build()
        val bitmap = Bitmap.createBitmap(
            // right is padding in preview image
            staticLayout.width + padding.toInt() + 20,
            (295 * App.instance.resources.displayMetrics.density).toInt(),
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawPaint(paint)
        canvas.save()
        canvas.translate(padding, padding) // Left top padding in preview image.
        staticLayout.draw(canvas)
        canvas.restore()
        return bitmap
    }
}
