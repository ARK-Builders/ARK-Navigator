package space.taran.arkbrowser.mvp.presenter

import android.view.View
import kotlinx.android.synthetic.main.dialog_roots_new.view.*
import space.taran.arkbrowser.mvp.model.entity.common.PredefinedIcon
import space.taran.arkbrowser.mvp.model.entity.common.Icon
import space.taran.arkbrowser.mvp.presenter.adapter.ReversibleItemGridPresenter
import space.taran.arkbrowser.mvp.view.item.FileItemView
import space.taran.arkbrowser.ui.adapter.ItemGridRVAdapter
import space.taran.arkbrowser.utils.findLongestCommonPrefix
import java.nio.file.Files
import java.nio.file.Path

import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.listDirectoryEntries

typealias PathHandler = (Path) -> Unit

@OptIn(ExperimentalPathApi::class)
class FolderPicker(
    paths: List<Path>,
    handler: PathHandler,
    private val view: View)
        : ItemGridRVAdapter<Path, Path>(InnerRootPicker(paths, handler)) {

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

class InnerRootPicker(paths: List<Path>, onClick: (Path) -> Unit):
    ReversibleItemGridPresenter<Path, Path>(
        findLongestCommonPrefix(paths).first,
        paths, onClick) {

    override fun bindView(view: FileItemView) {
        val path = items()[view.position()]
        view.setText(path.fileName.toString())

        view.setIcon(Icon.provide(path))
    }
}