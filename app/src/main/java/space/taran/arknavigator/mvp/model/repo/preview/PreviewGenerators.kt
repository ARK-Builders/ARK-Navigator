package space.taran.arknavigator.mvp.model.repo.preview

import android.graphics.Bitmap
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestOptions
import space.taran.arknavigator.mvp.model.repo.extra.ImageMetaExtra
import space.taran.arknavigator.mvp.model.repo.preview.generator.LinkPreviewGenerator
import space.taran.arknavigator.mvp.model.repo.preview.generator.PdfPreviewGenerator
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.utils.ImageUtils.glideExceptionListener
import space.taran.arknavigator.utils.LogTags.PREVIEWS
import space.taran.arknavigator.utils.extension
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.measureTimeMillis

object PreviewGenerators {

    private const val THUMBNAIL_SIZE = 128
    private const val COMPRESSION_QUALITY = 100

    // Use this map to declare new types of generators
    private var generatorsByExt: Map<String, (Path) -> Bitmap> = mapOf(
        "pdf" to { path: Path -> PdfPreviewGenerator.generate(path) },
        "link" to { path: Path -> LinkPreviewGenerator.generate(path) },
    )

    fun generate(path: Path, previewPath: Path, thumbnailPath: Path) {
        if (ImageMetaExtra.isImage(path)) {
            Log.d(PREVIEWS, "$path is an image, only generating a thumbnail for it")

            // images are special kind of a resource:
            // we don't need to store preview file for them,
            // we only need to downscale them into thumbnail
            val time1 = measureTimeMillis {
                val thumbnail = resizePreviewToThumbnail(path)
                storeThumbnail(thumbnailPath, thumbnail)
            }
            Log.d(PREVIEWS, "Thumbnail for image $path generated in $time1 ms")
            return
        }

        generatorsByExt[extension(path)]?.let { generator ->
            val time2 = measureTimeMillis {
                val preview = generator(path)
                val thumbnail = resizePreviewToThumbnail(preview)
                storePreview(previewPath, preview)
                storeThumbnail(thumbnailPath, thumbnail)
            }
            Log.d(
                PREVIEWS,
                "Preview and thumbnail generated for $path in $time2 ms"
            )
        } ?: Log.d(
            PREVIEWS,
            "No generators found for type .${extension(path)} ($path)"
        )
    }

    private fun storePreview(path: Path, bitmap: Bitmap) =
        storeImage(path, bitmap)

    private fun storeThumbnail(path: Path, bitmap: Bitmap) {
        if (bitmap.width > THUMBNAIL_SIZE) {
            throw AssertionError("Bitmap must be downscaled")
        }
        if (bitmap.height > THUMBNAIL_SIZE) {
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
            .apply(
                RequestOptions()
                    .downsample(DownsampleStrategy.CENTER_INSIDE)
                    .override(THUMBNAIL_SIZE)
                    .fitCenter()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
            )
            .addListener(glideExceptionListener<Bitmap>())
            .submit()
            .get()
}
