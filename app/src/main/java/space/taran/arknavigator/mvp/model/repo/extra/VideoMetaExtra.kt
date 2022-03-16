package space.taran.arknavigator.mvp.model.repo.extra

import android.media.MediaMetadataRetriever
import android.net.Uri
import space.taran.arknavigator.mvp.model.repo.index.MetaExtraTag
import space.taran.arknavigator.mvp.model.repo.index.ResourceMetaExtra
import space.taran.arknavigator.ui.App
import java.nio.file.Path

object VideoMetaExtra {
    val ACCEPTED_EXTENSIONS: Set<String> =
        setOf("mp4", "avi", "mov", "wmv", "flv")

    fun extract(path: Path): ResourceMetaExtra? {
        val result = mutableMapOf<MetaExtraTag, String>()
        val retriever = MediaMetadataRetriever()

        retriever.setDataSource(App.instance, Uri.fromFile(path.toFile()))
        val durationMillis = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        if (durationMillis != null) {
            result[MetaExtraTag.DURATION] = durationMillis
        }

        val width = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val height = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        if (width != null && height != null) {
            result[MetaExtraTag.WIDTH] = width
            result[MetaExtraTag.HEIGHT] = height
        }

        retriever.release()
        if (result.isEmpty()) {
            return null
        }

        return ResourceMetaExtra(result)
    }
}
