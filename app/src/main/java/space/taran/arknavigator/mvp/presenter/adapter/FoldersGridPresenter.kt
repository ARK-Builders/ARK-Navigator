package space.taran.arknavigator.mvp.presenter.adapter

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import space.taran.arknavigator.mvp.view.dialog.FolderPickerDialogView
import space.taran.arknavigator.mvp.view.item.FileItemView
import space.taran.arknavigator.utils.findLongestCommonPrefix
import space.taran.arknavigator.utils.listChildren

private class Frame(val folder: Path, val files: List<Path>)

class FoldersGridPresenter(
    val viewState: FolderPickerDialogView,
    val onFolderChanged: (folder: Path) -> Unit
) {
    private val frames = ArrayDeque<Frame>()

    fun currentFolder() = frames.last().folder

    fun getCount() = frames.lastOrNull()?.files?.size ?: 0

    fun bindView(view: FileItemView) {
        val path = frames.last().files[view.position()]

        view.setText(path.fileName.toString())

        if (Files.isDirectory(path)) {
            view.setFolderIcon()
        } else {
            view.setGenericIcon(path)
        }
    }

    fun init(paths: List<Path>) {
        val folder = findLongestCommonPrefix(paths)
        if (paths.size > 1) {
            frames.addLast(Frame(folder, paths))
        } else {
            val (directories, files) = listChildren(folder)
            val children = mutableListOf<Path>()
            children.addAll(directories.sorted())
            children.addAll(files.sorted())
            frames.addLast(Frame(folder, children))
        }
        viewState.updateFolders()
        onFolderChanged(folder)
    }

    fun onItemClick(pos: Int) {
        val folder = frames.last().files[pos]
        if (!folder.isDirectory()) {
            viewState.notifyFileChosenAsRoot()
            return
        }

        try {
            val (directories, files) = listChildren(frames.last().files[pos])
            val children = mutableListOf<Path>()
            children.addAll(directories.sorted())
            children.addAll(files.sorted())

            frames.addLast(Frame(folder, children))
            onFolderChanged(currentFolder())
            viewState.updateFolders()
        } catch (e: java.nio.file.AccessDeniedException) {
            viewState.showToast("Access denied.")
        }
    }

    fun onBackClick(): Boolean {
        frames.removeLast()
        if (frames.isEmpty()) return false

        viewState.updateFolders()
        onFolderChanged(currentFolder())
        return true
    }
}
