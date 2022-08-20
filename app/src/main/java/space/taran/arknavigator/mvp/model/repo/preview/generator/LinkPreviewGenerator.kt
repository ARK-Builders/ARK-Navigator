package space.taran.arknavigator.mvp.model.repo.preview.generator

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import space.taran.arklib.loadLinkPreview
import java.nio.file.Path
import kotlin.io.path.pathString

object LinkPreviewGenerator : PreviewGenerator() {
    override val acceptedExtensions = setOf("link")
    override val acceptedMimeTypes = emptySet<String>()

    override fun generate(path: Path, previewPath: Path, thumbnailPath: Path) {
        val preview = generatePreview(path)
        storePreview(previewPath, preview)
        val thumbnail = resizePreviewToThumbnail(preview)
        storeThumbnail(thumbnailPath, thumbnail)
    }

    private fun generatePreview(source: Path): Bitmap {
        val previewBytes =
            loadLinkPreview(source.pathString) ?: error("No image inside .link file")
        if (previewBytes.isEmpty()) {
            error("Image inside .link file was empty")
        }
        return BitmapFactory.decodeByteArray(previewBytes, 0, previewBytes.size)
    }
}
