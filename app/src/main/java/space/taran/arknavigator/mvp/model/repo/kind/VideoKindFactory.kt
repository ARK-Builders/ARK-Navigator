package space.taran.arknavigator.mvp.model.repo.kind

import android.media.MediaMetadataRetriever
import android.net.Uri
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.ui.App
import java.nio.file.Path

object VideoKindFactory : ResourceKindFactory<ResourceKind.Video> {
    override val acceptedExtensions: Set<String> =
        setOf("mp4", "avi", "mov", "wmv", "flv")
    override val acceptedKindCode = KindCode.Video

    override fun fromPath(path: Path): ResourceKind.Video {
        val retriever = MediaMetadataRetriever()

        retriever.setDataSource(App.instance, Uri.fromFile(path.toFile()))
        val durationMillis = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val duration = durationMillis?.toLong()

        val width = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            ?.toLong()
        val height = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
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
