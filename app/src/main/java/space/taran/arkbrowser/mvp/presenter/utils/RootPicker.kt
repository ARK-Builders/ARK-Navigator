package space.taran.arkbrowser.mvp.presenter.utils

import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.dialog_roots_new.view.*
import space.taran.arkbrowser.mvp.model.entity.common.Icon
import space.taran.arkbrowser.mvp.model.entity.common.IconOrImage
import space.taran.arkbrowser.mvp.presenter.adapter.ReversibleItemGridPresenter
import space.taran.arkbrowser.mvp.view.item.FileItemView
import space.taran.arkbrowser.ui.adapter.ItemGridRVAdapter
import space.taran.arkbrowser.utils.ROOT_PICKER
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.toList

typealias PathHandler = (Path) -> Unit

class RootPicker(
    paths: List<Path>,
    handler: PathHandler,
    private val view: View)
        : ItemGridRVAdapter<String, Path>(InnerRootPicker(paths, handler)) {

    init {
        view.rv_roots_dialog.adapter = this
        view.tv_roots_dialog_path.text = "/"
    }

    override fun backClicked(): String? {
        val label = super.backClicked()
        if (label != null) {
            view.tv_roots_dialog_path.text = label
        }
        return label
    }

    fun updatePath(path: Path) {
        val children = Files.list(path).toList()
        val label = path.toString()

        this.updateItems(label, children)
        view.tv_roots_dialog_path.text = label
    }
}

class InnerRootPicker(paths: List<Path>, pickRoot: (Path) -> Unit):
    ReversibleItemGridPresenter<String, Path>("/", paths, onClick(pickRoot)) {

    companion object {
        fun onClick(pickRoot: PathHandler): PathHandler = {
            Log.d(ROOT_PICKER, "path $it clicked")

            if (Files.isDirectory(it)) {
                Log.d(ROOT_PICKER, "and it is a folder, passing up")
                pickRoot(it)
            } else {
                Log.d(ROOT_PICKER, "but it is not a folder, ignoring")
            }
        }
    }

    override fun bindView(view: FileItemView) {
        val path = items()[view.pos]
        view.setText(path.fileName.toString())

        if (Files.isDirectory(path)) {
            view.setIcon(IconOrImage(icon = Icon.FOLDER))
        } else {
            view.setIcon(IconOrImage(icon = Icon.FILE))
            //todo
//                if (path.isImage())
//                    view.setIcon(Icon.IMAGE, path.file)
//                else
//                    view.setIcon(Icon.FILE, path.file)
        }
    }
}