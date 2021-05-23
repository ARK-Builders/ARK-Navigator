package space.taran.arkbrowser.mvp.presenter

import android.view.View
import kotlinx.android.synthetic.main.dialog_roots_new.view.*
import space.taran.arkbrowser.mvp.presenter.adapter.FoldersWalker
import space.taran.arkbrowser.mvp.presenter.adapter.ItemClickHandler
import space.taran.arkbrowser.ui.adapter.FilesReversibleRVAdapter
import java.nio.file.Path

import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.listDirectoryEntries

@OptIn(ExperimentalPathApi::class)
class FolderPicker(
    paths: List<Path>,
    handler: ItemClickHandler<Path>,
    private val view: View)
    : FilesReversibleRVAdapter<Path, Path>(FoldersWalker(paths, handler)) {

    init {
        view.rv_roots_dialog.adapter = this
        view.tv_roots_dialog_path.text = super.getLabel().toString()
    }

    override fun backClicked(): Path? {
        val label = super.backClicked()
        if (label != null) {
            view.tv_roots_dialog_path.text = label.toString()
        }
        return label
    }

    fun updatePath(path: Path) {
        val children = path.listDirectoryEntries().sorted()
        this.updateItems(path, children)

        view.tv_roots_dialog_path.text = path.toString()
    }
}