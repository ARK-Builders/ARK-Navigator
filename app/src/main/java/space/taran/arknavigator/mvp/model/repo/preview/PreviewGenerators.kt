package space.taran.arknavigator.mvp.model.repo.preview

import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.RequestOptions
import space.taran.arknavigator.mvp.model.repo.extra.ImageMetaExtra
import space.taran.arknavigator.mvp.model.repo.preview.generator.PdfPreviewGenerator
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.utils.extension
import java.nio.file.Files
import java.nio.file.Path

object PreviewGenerators {

    private const val THUMBNAIL_WIDTH = 72
    private const val THUMBNAIL_HEIGHT = 128
    private const val COMPRESSION_QUALITY = 100

    // Use this map to declare new types of generators
    private var generatorsByExt: Map<String, (Path) -> Bitmap> = mapOf(
        "pdf" to { path: Path -> PdfPreviewGenerator.generate(path) }
    )

    fun generate(path: Path, previewPath: Path, thumbnailPath: Path) {
        val ext = extension(path)

        if (ImageMetaExtra.ACCEPTED_EXTENSIONS.contains(ext)) {
            // images are special kind of a resource:
            // we don't need to store preview file for them,
            // we only need to downscale them into thumbnail
            val thumbnail = resizePreviewToThumbnail(path)
            storeThumbnail(thumbnailPath, thumbnail)
            return
        }

        generatorsByExt[ext]?.let { generator ->
            val preview = generator(path)
            val thumbnail = resizePreviewToThumbnail(preview)
            storePreview(previewPath, preview)
            storeThumbnail(thumbnailPath, thumbnail)
        }
    }

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

    private fun resizePreviewToThumbnail(preview: Bitmap): Bitmap =
        resizeToThumbnail(bitmapBuilder().load(preview))

    private fun resizePreviewToThumbnail(path: Path): Bitmap =
        resizeToThumbnail(bitmapBuilder().load(path.toFile()))

    private fun bitmapBuilder(): RequestBuilder<Bitmap> =
        Glide.with(App.instance)
            .asBitmap()

    private fun resizeToThumbnail(builder: RequestBuilder<Bitmap>): Bitmap =
        builder
            .apply(RequestOptions().override(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT))
            .fitCenter()
            .submit()
            .get()
}
