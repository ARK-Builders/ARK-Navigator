package space.taran.arknavigator.ui.adapter

import space.taran.arknavigator.databinding.DialogRootsNewBinding
import space.taran.arknavigator.mvp.presenter.adapter.FoldersWalker
import space.taran.arknavigator.mvp.presenter.adapter.ItemClickHandler
import space.taran.arknavigator.ui.App
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.listDirectoryEntries

@OptIn(ExperimentalPathApi::class)
class FolderPicker(
    paths: List<Path>,
    handler: ItemClickHandler<Path>,
    private val dialogBinding: DialogRootsNewBinding
) : FilesReversibleRVAdapter<Path, Path>(FoldersWalker(paths, handler).apply {
    App.instance.appComponent.inject(this)
}) {

    init {
        dialogBinding.rvRootsDialog.adapter = this
        dialogBinding.tvRootsDialogPath.text = super.getLabel().toString()
    }

    override fun backClicked(): Path? {
        val label = super.backClicked()
        if (label != null) {
            dialogBinding.tvRootsDialogPath.text = label.toString()
        }
        return label
    }

    fun updatePath(path: Path) {
        val children = path.listDirectoryEntries().sorted()
        this.updateItems(path, children)

        dialogBinding.tvRootsDialogPath.text = path.toString()
    }
}