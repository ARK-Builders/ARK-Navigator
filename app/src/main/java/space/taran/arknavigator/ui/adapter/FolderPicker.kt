package space.taran.arknavigator.ui.adapter

import android.view.View
import androidx.viewbinding.ViewBinding
import space.taran.arknavigator.databinding.DialogRootsNewBinding
import space.taran.arknavigator.mvp.presenter.adapter.FoldersWalker
import space.taran.arknavigator.mvp.presenter.adapter.ItemClickHandler
import java.nio.file.Path

import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.listDirectoryEntries

@OptIn(ExperimentalPathApi::class)
class FolderPicker(
    paths: List<Path>,
    handler: ItemClickHandler<Path>,
    private val binding: DialogRootsNewBinding)
    : FilesReversibleRVAdapter<Path, Path>(FoldersWalker(paths, handler)) {

    init {
        binding.rvRootsDialog.adapter = this
        binding.tvRootsDialogPath.text = super.getLabel().toString()
    }

    override fun backClicked(): Path? {
        val label = super.backClicked()
        if (label != null) {
            binding.tvRootsDialogPath.text = label.toString()
        }
        return label
    }

    fun updatePath(path: Path) {
        val children = path.listDirectoryEntries().sorted()
        this.updateItems(path, children)

        binding.tvRootsDialogPath.text = path.toString()
    }
}