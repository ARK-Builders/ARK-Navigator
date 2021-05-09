package space.taran.arkbrowser.mvp.presenter.utils

import android.util.Log
import space.taran.arkbrowser.mvp.model.entity.common.Icon
import space.taran.arkbrowser.mvp.model.entity.common.IconOrImage
import space.taran.arkbrowser.mvp.presenter.adapter.ReversibleItemGridPresenter
import space.taran.arkbrowser.mvp.view.item.FileItemView
import space.taran.arkbrowser.utils.ROOT_PICKER
import java.nio.file.Files
import java.nio.file.Path

typealias PathHandler = (Path) -> Unit

class RootPicker(paths: List<Path>, pickRoot: (Path) -> Unit):
    ReversibleItemGridPresenter<Path>(paths, onClick(pickRoot)) {

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