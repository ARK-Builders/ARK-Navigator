package space.taran.arknavigator.ui.extra

import android.widget.TextView
import space.taran.arknavigator.mvp.model.repo.kind.ResourceKind
import space.taran.arknavigator.utils.extensions.textOrGone

object VideoExtraLoader {
    fun load(
        video: ResourceKind.Video,
        resolutionTV: TextView,
        durationTV: TextView
    ) {
        val width = video.width
        val height = video.height

        if (width != null && height != null) {
            resolutionTV.textOrGone(
                qualityTextCode(
                    width.toInt(),
                    height.toInt()
                )
            )
        }

        val duration = video.duration
        if (duration != null) {
            durationTV.textOrGone(durationTextCode(duration))
        }
    }

    private fun durationTextCode(millis: Long): String {
        // use `Duration.ofMillis(timeMillis).secondsPart()` in API 31

        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        fun toText(n: Long): String =
            if (n < 10) {
                "0$n"
            } else {
                "$n"
            }

        val minAndSec = "${toText(minutes % 60)}:${toText(seconds % 60)}"
        if (hours > 0) {
            return "${toText(hours)}:$minAndSec"
        }
        return minAndSec
    }

    private fun qualityTextCode(width: Int, height: Int): String {
        return when (width to height) {
            256 to 144 -> "144p"
            426 to 240 -> "240p"
            640 to 360 -> "360p"
            854 to 480 -> "480p"
            1280 to 720 -> "720p"
            1920 to 1080 -> "1080p"
            2560 to 1440 -> "1440p"
            3840 to 2160 -> "2160p"
            else -> "${width}x$height"
        }
    }
}
