package space.taran.arknavigator.mvp.model.repo.kind

import android.net.Uri
import android.util.Log
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.utils.LogTags
import wseemann.media.FFmpegMediaMetadataRetriever
import java.nio.file.Path
import kotlin.io.path.name

object VideoKindFactory : ResourceKindFactory<ResourceKind.Video> {
    override val acceptedExtensions: Set<String> =
        setOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "ts", "mpg")
    override val acceptedMimeTypes: Set<String> =
        setOf("video/mp4")
    override val acceptedKindCode = KindCode.VIDEO

    override fun fromPath(path: Path): ResourceKind.Video {
        val retriever = FFmpegMediaMetadataRetriever()

        try {
            retriever.setDataSource(App.instance, Uri.fromFile(path.toFile()))
        } catch (e: IllegalArgumentException) {
            Log.e(LogTags.PREVIEWS, "Failed to setDataSource for ${path.name}")
        }
        val durationMillis = retriever
            .extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION)
        val duration = durationMillis?.toLong()

        val width = retriever
            .extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            ?.toLong()
        val height = retriever
            .extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            ?.toLong()

        retriever.release()

        return ResourceKind.Video(height, width, duration)
    }

    override fun fromRoom(extras: Map<MetaExtraTag, String>): ResourceKind.Video =
        ResourceKind.Video(
            extras[MetaExtraTag.HEIGHT]?.toLong(),
            extras[MetaExtraTag.WIDTH]?.toLong(),
            extras[MetaExtraTag.DURATION]?.toLong()
        )

    override fun toRoom(
        id: ResourceId,
        kind: ResourceKind.Video
    ): Map<MetaExtraTag, String?> =
        mapOf(
            MetaExtraTag.HEIGHT to kind.height?.toString(),
            MetaExtraTag.WIDTH to kind.width?.toString(),
            MetaExtraTag.DURATION to kind.duration?.toString()
        )
}
