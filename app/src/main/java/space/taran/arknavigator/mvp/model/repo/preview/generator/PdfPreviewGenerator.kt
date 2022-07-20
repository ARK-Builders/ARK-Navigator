package space.taran.arknavigator.mvp.model.repo.preview.generator

import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import space.taran.arknavigator.ui.App
import space.taran.arklib.pdfPreviewGenerate
import space.taran.arklib.PreviewQuality
import java.io.FileInputStream
import java.nio.file.Path

object PdfPreviewGenerator : PreviewGenerator() {
    override val acceptedExtensions = setOf("pdf")
    override val acceptedMimeTypes = setOf("application/pdf")

    override fun generate(path: Path, previewPath: Path, thumbnailPath: Path) {
        val preview = generatePreview(path)
        storePreview(previewPath, preview)
        val thumbnail = resizePreviewToThumbnail(preview)
        storeThumbnail(thumbnailPath, thumbnail)
    }

    private fun generatePreview(source: Path): Bitmap {
        val page = 0

        val finalContext = App.instance

        val fd: ParcelFileDescriptor? =
            finalContext
                .contentResolver
                .openFileDescriptor(Uri.fromFile(source.toFile()), "r")
        val stream = FileInputStream(fd?.fileDescriptor).readBytes()

        val bitmap = pdfPreviewGenerate(stream, PreviewQuality.HIGH)

        return bitmap
    }
}
