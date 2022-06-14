package space.taran.arknavigator.mvp.model.repo.preview

import android.graphics.Bitmap
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestOptions
import space.taran.arknavigator.mvp.model.repo.kind.ImageKindFactory
import space.taran.arknavigator.mvp.model.repo.kind.PlainTextKindFactory
import space.taran.arknavigator.mvp.model.repo.preview.generator.LinkPreviewGenerator
import space.taran.arknavigator.mvp.model.repo.preview.generator.PdfPreviewGenerator
import space.taran.arknavigator.mvp.model.repo.preview.generator.TxtPreviewGenerator
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.utils.ImageUtils.glideExceptionListener
import space.taran.arknavigator.utils.LogTags.PREVIEWS
import space.taran.arknavigator.utils.extension
import space.taran.arknavigator.utils.getMimeTypeUsingTika
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.measureTimeMillis

object PreviewGenerators {

    private const val THUMBNAIL_SIZE = 128
    private const val COMPRESSION_QUALITY = 100

    // Use this map to declare new types of generators
    private val generatorsByExt: Map<String, (Path) -> Bitmap> = mapOf(
        "pdf" to { path: Path -> PdfPreviewGenerator.generate(path) },
        "link" to { path: Path -> LinkPreviewGenerator.generate(path) },
        "txt" to { path: Path -> TxtPreviewGenerator.generate(path, THUMBNAIL_SIZE) }
    )

    fun generate(path: Path, previewPath: Path, thumbnailPath: Path) {
        if (ImageKindFactory.isValid(path)) {
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

        val ext = extension(path)
        generatorsByExt[ext]?.let { generator ->
            val time2 = measureTimeMillis {
                val preview = generator(path)
                storePreview(previewPath, preview)
                val thumbnail = resizePreviewToThumbnail(preview)
                storeThumbnail(thumbnailPath, thumbnail)
            }
            Log.d(
                PREVIEWS,
                "Preview and thumbnail generated for $path in $time2 ms"
            )
            return
        } ?: Log.d(
            PREVIEWS,
            "No generators found for type .${extension(path)} ($path)"
        )
        // This section called when the code will failed to generate preview for
        // pdf, link, txt extension.This code will match with existing factories'
        // acceptedExtensions as well as acceptedMimeTypes (if ext is blank).
        if (PlainTextKindFactory.isValid(path)) {
            generatorsByExt["txt"]?.let { generator ->
                val time2 = measureTimeMillis {
                    val thumbnail = resizePreviewToThumbnail(generator(path))
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
            return
        } else if (getMimeTypeUsingTika(path = path) == "application/pdf") {
            generatorsByExt["pdf"]?.let { generator ->
                val time2 = measureTimeMillis {
                    val preview = generator(path)
                    storePreview(previewPath, preview)
                    val thumbnail = resizePreviewToThumbnail(preview)
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
            return
        }
        Log.d(
            PREVIEWS,
            "GetFileTypeUsingTika ${getMimeTypeUsingTika(path = path)}"
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
