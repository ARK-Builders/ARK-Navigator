package space.taran.arknavigator.mvp.model.fsmonitoring

import space.taran.arknavigator.utils.FSEventLogger
import java.io.File
import java.util.*
import kotlin.io.path.Path

class RecursiveDirectoryObserver(
    private val fsMonitoring: FSMonitoring,
    private val root: String
) {
    var directoryObservers: MutableList<DirectoryObserver> = mutableListOf()

    fun startWatching() {
        directoryObservers = mutableListOf()
        val stack: Stack<String> = Stack()
        stack.push(root)
        while (!stack.empty()) {
            val parent: String = stack.pop()
            directoryObservers.add(DirectoryObserver.create(this, parent))
            val path = File(parent)
            val files: Array<File> = path.listFiles() ?: continue
            for (i in files.indices) {
                if (files[i].isDirectory && !files[i].name.equals(".")
                    && !files[i].name.equals("..")
                ) {
                    stack.push(files[i].path)
                }
            }
        }
        for (i in directoryObservers.indices) directoryObservers[i].startWatching()
    }

    fun onEvent(directory: String, event: Int, eventPath: String?) {
        FSEventLogger.log(directory, event, eventPath)
        fsMonitoring.onEvent(Path(root), Path(directory), event, eventPath?.let { Path(it) })
    }
}