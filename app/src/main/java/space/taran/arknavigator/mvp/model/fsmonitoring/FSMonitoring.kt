package space.taran.arknavigator.mvp.model.fsmonitoring

import android.os.FileObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import space.taran.arknavigator.mvp.model.IndexCache
import space.taran.arknavigator.mvp.model.TagsCache
import space.taran.arknavigator.utils.isTagsStorage
import java.nio.file.Path

class FSMonitoring(
    val indexCache: IndexCache,
    val tagsCache: TagsCache,
    val appScope: CoroutineScope
) {
    private val rootObservers = mutableListOf<RecursiveDirectoryObserver>()

    fun startWatchingRoot(root: String) {
        val obs = RecursiveDirectoryObserver(this, root)
        rootObservers.add(obs)
        obs.startWatching()
    }

    fun onEvent(root: Path, directory: Path, event: Int, eventPath: Path?) {
        when (event) {
            FileObserver.CREATE -> {}
            FileObserver.CLOSE_WRITE -> onCloseWrite(root, directory, eventPath!!)
            FileObserver.DELETE -> onDelete(root, directory, eventPath!!)
            FileObserver.MOVE_SELF -> {}
            FileObserver.MOVED_FROM -> onMovedFrom(root, directory, eventPath!!)
            FileObserver.MOVED_TO -> onMovedTo(root, directory, eventPath!!)
            FileObserver.DELETE_SELF -> {}
        }
    }

    private fun onCloseWrite(root: Path, directory: Path, eventPath: Path) = appScope.launch(Dispatchers.Default) {
        if (!isTagsStorage(eventPath)) {
            val index = indexCache.onResourceModified(root, eventPath)
            tagsCache.onIndexChanged(root, index)
        }
    }

    private fun onDelete(root: Path, directory: Path, eventPath: Path) = appScope.launch(Dispatchers.Default) {
        val id = indexCache.onResourceDeleted(root, eventPath)
        tagsCache.remove(id)
    }

    private fun onMovedFrom(root: Path, directory: Path, eventPath: Path) = appScope.launch(Dispatchers.Default) {
        indexCache.onResourceDeleted(root, eventPath)
    }

    private fun onMovedTo(root: Path, directory: Path, eventPath: Path) = appScope.launch(Dispatchers.Default) {
        indexCache.onResourceCreated(root, eventPath)
    }
}