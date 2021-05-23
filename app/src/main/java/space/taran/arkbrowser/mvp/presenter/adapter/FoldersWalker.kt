package space.taran.arkbrowser.mvp.presenter.adapter

import space.taran.arkbrowser.mvp.model.dao.common.Preview
import space.taran.arkbrowser.mvp.view.item.FileItemView
import space.taran.arkbrowser.utils.findLongestCommonPrefix
import java.nio.file.Path

class FoldersWalker(paths: List<Path>, onClick: ItemClickHandler<Path>)
    : ItemsReversiblePresenter<Path, Path, FileItemView>(
        findLongestCommonPrefix(paths).first, paths, onClick) {

    override fun bindView(view: FileItemView) {
        val path = items()[view.position()]
        view.setText(path.fileName.toString())

        view.setIcon(Preview.provide(path))
    }
}