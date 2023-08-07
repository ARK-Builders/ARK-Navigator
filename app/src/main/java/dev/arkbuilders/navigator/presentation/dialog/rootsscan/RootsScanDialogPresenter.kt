package dev.arkbuilders.navigator.presentation.dialog.rootsscan

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.MvpPresenter
import moxy.presenterScope
import space.taran.arklib.arkFolder
import dev.arkbuilders.navigator.data.utils.LogTags
import dev.arkbuilders.navigator.data.utils.listDevices
import java.nio.file.Path
import java.util.LinkedList
import java.util.Queue
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries

class RootsScanDialogPresenter : MvpPresenter<RootsScanView>() {

    private val roots = mutableListOf<Path>()
    private val queue: Queue<Path> = LinkedList()

    override fun onFirstViewAttach() {
        viewState.init()
    }

    fun onScanBtnClick() {
        viewState.startScan()
        scan()
    }

    fun onEnoughBtnClick() {
        viewState.notifyRootsFound(roots)
        viewState.closeDialog()
    }

    private fun scan() = presenterScope.launch(Dispatchers.IO) {
        queue.addAll(listDevices())

        while (queue.isNotEmpty()) {
            ensureActive()
            scanFolder(queue.poll()!!)
        }

        withContext(Dispatchers.Main) {
            viewState.notifyRootsFound(roots)
            viewState.scanCompleted(roots.size)
        }
    }

    private suspend fun scanFolder(folder: Path) = try {
        if (folder.arkFolder().exists()) {
            roots.add(folder)
            withContext(Dispatchers.Main) { viewState.setProgress(roots.size) }
        } else
            queue.addAll(folder.listDirectoryEntries().filter(Path::isDirectory))
    } catch (e: Exception) {
        Log.w(LogTags.FILES, "Can't scan $folder due to $e")
        withContext(Dispatchers.Main) {
            viewState.toastFolderSkip(folder)
        }
    }
}
