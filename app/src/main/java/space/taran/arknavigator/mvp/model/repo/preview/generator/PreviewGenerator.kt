package space.taran.arknavigator.mvp.model.repo.preview.generator

import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import space.taran.arknavigator.mvp.model.repo.index.ResourceKind
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.preview.Preview
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.utils.extension
import java.nio.file.Files
import java.nio.file.Path

object PreviewGenerator {

    fun generate(path: Path, meta: ResourceMeta, previewPath: Path, thumbnailPath: Path): Preview =
        when (meta.kind) {
            ResourceKind.IMAGE -> generateImagePreview(path, meta, thumbnailPath)
            ResourceKind.VIDEO -> generateVideoPreview(meta)
            ResourceKind.DOCUMENT -> generateDocumentPreview(path, meta, previewPath, thumbnailPath)
            null -> Preview(null, null, meta)
        }

    private fun generateImagePreview(
        path: Path,
        meta: ResourceMeta,
        thumbnailPath: Path
    ): Preview {
        val thumbnail = generateThumbnail(path)
        storeThumbnail(thumbnailPath, thumbnail)
        return Preview(path, thumbnailPath, meta)
    }

    private fun generateVideoPreview(meta: ResourceMeta) = Preview(null, null, meta)

    private fun generateDocumentPreview(
        path: Path,
        meta: ResourceMeta,
        previewPath: Path,
        thumbnailPath: Path
    ): Preview {
        val preview = when(extension(path)) {
            "pdf" -> PdfPreviewGenerator.generate(path)
            else -> null
        } ?: return Preview(null, null, meta)

        val thumbnail = generateThumbnail(preview)
        storePreview(previewPath, preview)
        storeThumbnail(thumbnailPath, thumbnail)
        return Preview(previewPath, thumbnailPath, meta)
    }

    private fun generateThumbnail(bitmap: Bitmap): Bitmap = Glide.with(App.instance)
        .asBitmap()
        .load(bitmap)
        .apply(RequestOptions().override(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT))
        .fitCenter()
        .submit()
        .get()

    private fun generateThumbnail(path: Path): Bitmap = Glide.with(App.instance)
        .asBitmap()
        .load(path.toFile())
        .apply(RequestOptions().override(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT))
        .fitCenter()
        .submit()
        .get()

    private fun storePreview(path: Path, bitmap: Bitmap) =
        storeImage(path, bitmap)

    private fun storeThumbnail(path: Path, bitmap: Bitmap) {
        if (bitmap.width > THUMBNAIL_WIDTH) {
            throw AssertionError("Bitmap must be downscaled")
        }
        if (bitmap.height > THUMBNAIL_HEIGHT) {
            throw AssertionError("Bitmap must be downscaled")
        }

        storeImage(path, bitmap)
    }

    private fun storeImage(target: Path, bitmap: Bitmap) {
        Files.newOutputStream(target).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY, out)
            out.flush()
        }
    }

    private const val THUMBNAIL_WIDTH = 72
    private const val THUMBNAIL_HEIGHT = 128
    private const val COMPRESSION_QUALITY = 100
}