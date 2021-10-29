package space.taran.arknavigator.mvp.model.fsmonitoring

import android.os.Build
import android.os.FileObserver
import androidx.annotation.RequiresApi
import java.io.File

class DirectoryObserver: FileObserver {
    @RequiresApi(Build.VERSION_CODES.Q)
    constructor(directory: File): super(directory)
    constructor(directory: String): super(directory)

    private lateinit var recursiveObs: RecursiveDirectoryObserver
    private lateinit var directory: String

    override fun onEvent(event: Int, path: String?) {
        val eventCode = event and ALL_EVENTS
        val eventPath = path?.let { "$directory/$path" }
        recursiveObs.onEvent(directory, eventCode, eventPath)
    }

    companion object {
        fun create(recursiveObs: RecursiveDirectoryObserver, directory: String): DirectoryObserver {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                DirectoryObserver(File(directory)).also {
                    it.recursiveObs = recursiveObs
                    it.directory = directory
                }
            } else {
                DirectoryObserver(directory).also {
                    it.recursiveObs = recursiveObs
                    it.directory = directory
                }
            }
        }
    }
}