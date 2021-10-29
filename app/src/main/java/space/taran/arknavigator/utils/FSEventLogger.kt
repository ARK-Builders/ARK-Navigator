package space.taran.arknavigator.utils

import android.os.FileObserver
import timber.log.Timber

object FSEventLogger {
    private const val LOG_UNKNOWN_EVENTS = false

    fun log(directory: String, event: Int, eventPath: String?) {
        val eventName = provideEventName(event)
        eventName?.let {
            Timber.d("$eventName \n Directory: $directory \n Path: $eventPath")
        }
    }

    private fun provideEventName(event: Int): String? {
        return when(event) {
            FileObserver.ACCESS -> "ACCESS"
            FileObserver.ATTRIB -> "ATTRIB"
            FileObserver.CLOSE_NOWRITE -> "CLOSE_NOWRITE"
            FileObserver.CLOSE_WRITE -> "CLOSE_WRITE"
            FileObserver.CREATE -> "CREATE"
            FileObserver.DELETE -> "DELETE"
            FileObserver.DELETE_SELF -> "DELETE_SELF"
            FileObserver.MODIFY -> "MODIFY"
            FileObserver.MOVED_FROM -> "MOVED_FROM"
            FileObserver.MOVED_TO -> "MOVED_TO"
            FileObserver.MOVE_SELF -> "MOVE_SELF"
            FileObserver.OPEN -> "OPEN"
            else -> if (LOG_UNKNOWN_EVENTS) "Unknown event code $event" else null
        }
    }
}