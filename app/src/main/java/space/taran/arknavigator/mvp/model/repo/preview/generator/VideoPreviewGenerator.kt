package space.taran.arknavigator.mvp.model.repo.preview.generator

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import java.nio.file.Path
import space.taran.arknavigator.ui.App
import wseemann.media.FFmpegMediaMetadataRetriever

object VideoPreviewGenerator : PreviewGenerator() {
    override val acceptedExtensions: Set<String> =
        setOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "ts", "mpg")
    override val acceptedMimeTypes: Set<String>
        get() = setOf()

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

        try {
            retriever.setDataSource(App.instance, Uri.fromFile(path.toFile()))
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
        var bitmap: Bitmap? = retriever.frameAtTime

        if (bitmap != null) {
            val b2: Bitmap? = retriever.getFrameAtTime(
                4000000,
                FFmpegMediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
            if (b2 != null) {
                bitmap = b2
            }
        }

        if (bitmap != null) {
            Log.i("Thumbnail", "Extracted frame")
        } else {
            Log.e("Thumbnail", "Failed to extract frame")
        }

        retriever.release()

        return bitmap
    }
}
