package space.taran.arknavigator.mvp.model.repo.extra

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.TextView
import space.taran.arknavigator.mvp.model.repo.index.MetaExtraTag
import space.taran.arknavigator.mvp.model.repo.index.ResourceMetaExtra
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.utils.extensions.textOrGone
import java.nio.file.Path

object VideoMetaExtra {
    val ACCEPTED_EXTENSIONS: Set<String> =
        setOf("mp4", "avi", "mov", "wmv", "flv")

    fun extract(path: Path): ResourceMetaExtra? {
        val result = mutableMapOf<MetaExtraTag, Long>()
        val retriever = MediaMetadataRetriever()

        retriever.setDataSource(App.instance, Uri.fromFile(path.toFile()))
        val durationMillis = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        if (durationMillis != null) {
            result[MetaExtraTag.DURATION] = durationMillis.toLong()
        }

        val width = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val height = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        if (width != null && height != null) {
            result[MetaExtraTag.WIDTH] = width.toLong()
            result[MetaExtraTag.HEIGHT] = height.toLong()
        }

        retriever.release()
        if (result.isEmpty()) {
            return null
        }

        return ResourceMetaExtra(result)
    }

    fun draw(extra: ResourceMetaExtra, resolutionTV: TextView, durationTV: TextView) {
        val width = extra.data[MetaExtraTag.WIDTH]
        val height = extra.data[MetaExtraTag.HEIGHT]

        if (width != null && height != null) {
            resolutionTV.textOrGone(qualityTextCode(width.toInt(), height.toInt()))
        }

        val duration = extra.data[MetaExtraTag.DURATION]
        if (duration != null) {
            durationTV.textOrGone(durationTextCode(duration))
        }
    }

    private fun durationTextCode(millis: Long): String {
        //use `Duration.ofMillis(timeMillis).secondsPart()` in API 31

        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        fun toText(n: Long): String =
            when {
                n <= 0 -> ""
                n < 10 -> "0$n"
                else -> "$n"
            }

        val minAndSec = "${toText(minutes % 60)}:${toText(seconds % 60)}"
        if (hours > 0) {
            return "${toText(hours)}:$minAndSec"
        }
        return minAndSec
    }

    private fun qualityTextCode(width: Int, height: Int): String {
        return when (width to height) {
            256  to 144  -> "144p"
            426  to 240  -> "240p"
            640  to 360  -> "360p"
            854  to 480  -> "480p"
            1280 to 720  -> "720p"
            1920 to 1080 -> "1080p"
            2560 to 1440 -> "1440p"
            3840 to 2160 -> "2160p"
            else -> "${width}x${height}"
        }
    }
}