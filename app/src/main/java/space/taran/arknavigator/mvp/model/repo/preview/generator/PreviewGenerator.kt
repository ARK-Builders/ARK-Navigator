package space.taran.arknavigator.mvp.model.repo.preview.generator

import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestOptions
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.utils.ImageUtils
import space.taran.arknavigator.utils.extension
import java.nio.file.Files
import java.nio.file.Path

abstract class PreviewGenerator {
    abstract val acceptedExtensions: Set<String>
    abstract val acceptedMimeTypes: Set<String>

    fun isValid(path: Path) = acceptedExtensions.contains(extension(path))
    fun isValid(mimeType: String) = acceptedMimeTypes.contains(mimeType)

    abstract fun generate(path: Path, previewPath: Path, thumbnailPath: Path)

    protected fun storePreview(path: Path, bitmap: Bitmap) =
        storeImage(path, bitmap)

    protected fun storeThumbnail(path: Path, bitmap: Bitmap) {
        if (bitmap.width > THUMBNAIL_SIZE) {
            throw AssertionError("Bitmap must be downscaled")
        }
        if (bitmap.height > THUMBNAIL_SIZE) {
            throw AssertionError("Bitmap must be downscaled")
        }

        storeImage(path, bitmap)
    }

    protected fun resizePreviewToThumbnail(preview: Bitmap): Bitmap =
        resizeToThumbnail(bitmapBuilder().load(preview))

    protected fun resizePreviewToThumbnail(path: Path): Bitmap =
        resizeToThumbnail(bitmapBuilder().load(path.toFile()))

    private fun storeImage(target: Path, bitmap: Bitmap) {
        Files.newOutputStream(target).use { out ->
            bitmap.compress(
                Bitmap.CompressFormat.PNG,
                COMPRESSION_QUALITY, out
            )
            out.flush()
        }
    }

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
            .addListener(ImageUtils.glideExceptionListener<Bitmap>())
            .submit()
            .get()

    companion object {
        @JvmStatic
        protected val THUMBNAIL_SIZE = 128
        private const val COMPRESSION_QUALITY = 100
    }
}
