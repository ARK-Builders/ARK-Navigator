package space.taran.arknavigator.utils

import android.media.MediaMetadataRetriever
import android.net.Uri
import space.taran.arknavigator.mvp.model.repo.ExtraInfoTag
import space.taran.arknavigator.ui.App
import java.nio.file.Path
import java.util.concurrent.TimeUnit

fun getVideoInfo(filePath: Path?): Map<ExtraInfoTag, String> {
    val retriever = MediaMetadataRetriever()

    retriever.setDataSource(App.instance, Uri.fromFile(filePath?.toFile()))
    val timeMillis =
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
    val width =
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toLong() ?: 0L
    val height =
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toLong() ?: 0L


    var minutes = TimeUnit.MILLISECONDS.toMinutes(timeMillis).toString()
    val minutesPassedInMillis = TimeUnit.MINUTES.toMillis(minutes.toLong()).toString()
    var seconds =
        TimeUnit.MILLISECONDS.toSeconds(timeMillis - minutesPassedInMillis.toLong()).toString()

    if (minutes.length == 1) minutes = "0$minutes"
    if (seconds.length == 1) seconds = "0$seconds"

    val duration = "$minutes:$seconds"
    val resolution = qualityTextCode(width, height)

    val result = mutableMapOf<ExtraInfoTag, String>()
    result[ExtraInfoTag.MEDIA_DURATION] = duration

    if (resolution != null)
        result[ExtraInfoTag.MEDIA_RESOLUTION] = resolution

    retriever.release()
    return result
}

fun qualityTextCode(width: Long, height: Long): String? {
    val resolutionPair = listOf(width, height)

    return when {
        resolutionPair.containsAll(listOf(256, 144)) -> "144p"
        resolutionPair.containsAll(listOf(426, 240)) -> "240p"
        resolutionPair.containsAll(listOf(640, 360)) -> "360p"
        resolutionPair.containsAll(listOf(854, 480)) -> "480p"
        resolutionPair.containsAll(listOf(1280, 720)) -> "720p"
        resolutionPair.containsAll(listOf(1920, 1080)) -> "1080p"
        resolutionPair.containsAll(listOf(2560, 1440)) -> "1440p"
        resolutionPair.containsAll(listOf(3840, 2160)) -> "2160p"
        else -> null
    }
}