package space.taran.arknavigator.mvp.model.repo.preview.generator

import android.graphics.Bitmap
import space.taran.arklib.pdfPreviewGenerate
import space.taran.arklib.PreviewQuality
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
        // TODO: quality must be configurable in preferences screen
        return pdfPreviewGenerate(source.toString(), PreviewQuality.HIGH)
    }
}
