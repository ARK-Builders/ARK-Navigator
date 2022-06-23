package space.taran.arknavigator.mvp.model.repo.preview.generator

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.nio.file.Path
import java.util.zip.ZipFile

object LinkPreviewGenerator : PreviewGenerator() {
    override val acceptedExtensions = setOf("link")
    override val acceptedMimeTypes = emptySet<String>()
    private const val IMAGE_FILE = "link.png"

    override fun generate(path: Path, previewPath: Path, thumbnailPath: Path) {
        val preview = generatePreview(path)
        storePreview(previewPath, preview)
        val thumbnail = resizePreviewToThumbnail(preview)
        storeThumbnail(thumbnailPath, thumbnail)
    }

    private fun generatePreview(source: Path): Bitmap {
        val zip = ZipFile(source.toFile())
        val entries = zip.entries()
        val imageEntry = entries
            .asSequence()
            .find { entry -> entry.name == IMAGE_FILE }
            ?: error("No image inside .link file")

        return BitmapFactory.decodeStream(zip.getInputStream(imageEntry))
    }
}
