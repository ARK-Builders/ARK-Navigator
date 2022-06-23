package space.taran.arknavigator.mvp.model.repo.preview.generator

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import space.taran.arknavigator.ui.App
import java.io.FileReader
import java.nio.file.Path

object TxtPreviewGenerator : PreviewGenerator() {
    override val acceptedExtensions = setOf("txt")
    override val acceptedMimeTypes = setOf("text/plain")

    override fun generate(path: Path, previewPath: Path, thumbnailPath: Path) {
        val thumbnail = generateThumbnail(path)
        storeThumbnail(thumbnailPath, thumbnail)
    }

    // it is padding in preview image
    private val padding = 2f * App.instance.resources.displayMetrics.density

    private fun generateThumbnail(source: Path): Bitmap {
        val fr = FileReader(source.toFile())
        val textPaint = TextPaint()
        textPaint.isAntiAlias = true
        textPaint.textSize = 5f
        textPaint.color = -0x1000000

        val numberOfLines = fr.readLines()
        val text = if (numberOfLines.size > 10)
            numberOfLines.subList(0, 10).joinToString("\n")
        else numberOfLines.subList(0, numberOfLines.size).joinToString("\n")
        val staticLayout = StaticLayout.Builder.obtain(
            text,
            0,
            text.length,
            textPaint,
            // right is padding in preview image
            THUMBNAIL_SIZE - (padding.toInt())
        ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(5f, 0.9f)
            .setIncludePad(true)
            .build()
        val bitmap = Bitmap.createBitmap(
            THUMBNAIL_SIZE,
            THUMBNAIL_SIZE,
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
