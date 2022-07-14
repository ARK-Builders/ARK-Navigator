package space.taran.arknavigator.mvp.model.repo.preview.generator

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.nio.file.Path
import kotlin.io.path.name
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.utils.LogTags.PREVIEWS
import wseemann.media.FFmpegMediaMetadataRetriever

object VideoPreviewGenerator : PreviewGenerator() {
    override val acceptedExtensions: Set<String> =
        setOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "ts", "mpg")
    override val acceptedMimeTypes: Set<String>
        get() = setOf("video/mp4")

    override fun generate(path: Path, previewPath: Path, thumbnailPath: Path) {
        val preview = generatePreview(path)
        preview?.let {
            storePreview(previewPath, it)
            val thumbnail = resizePreviewToThumbnail(it)
            storeThumbnail(thumbnailPath, thumbnail)
        }
    }

    private fun generatePreview(path: Path): Bitmap? {
        val retriever = FFmpegMediaMetadataRetriever()
        var mainBitmap: Bitmap? = null
        try {
            retriever.setDataSource(App.instance, Uri.fromFile(path.toFile()))
            // Trying 3 ways to get preview image for video.
            // 1. using FFmpegMediaMetadataRetriever
            // 2. using MediaMetadataRetriever
            // 3. using Glide
            mainBitmap = retriever.frameAtTime ?: let {
                MediaMetadataRetriever().let { mediaMetadataRetriever ->
                    try {
                        mediaMetadataRetriever.setDataSource(
                            App.instance, Uri.fromFile(path.toFile())
                        )
                    } catch (e: IllegalArgumentException) {
                        Log.e(
                            PREVIEWS, "Failed to setDataSource for ${path.name}"
                        )
                    }
                    var bitmap: Bitmap? = mediaMetadataRetriever.frameAtTime
                    if (bitmap != null) {
                        val tempBitmap: Bitmap? = mediaMetadataRetriever
                            .getFrameAtTime(
                                4000000,
                                FFmpegMediaMetadataRetriever.OPTION_CLOSEST_SYNC
                            )
                        if (tempBitmap != null) {
                            bitmap = tempBitmap
                        }
                    }
                    bitmap
                } ?: Glide.with(App.instance.baseContext).asBitmap()
                    .load(path.toFile())
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .submit().get()
            }
            if (mainBitmap != null) {
                val bitmap: Bitmap? = retriever.getFrameAtTime(
                    4000000,
                    FFmpegMediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                if (bitmap != null) {
                    mainBitmap = bitmap
                }
            }

            if (mainBitmap != null) {
                Log.i("Thumbnail", "Extracted frame")
            } else {
                Log.e("Thumbnail", "Failed to extract frame")
            }
        } catch (e: IllegalArgumentException) {
            Log.e(PREVIEWS, "Failed to setDataSource for ${path.name}")
        }
        retriever.release()
        return mainBitmap
    }
}
