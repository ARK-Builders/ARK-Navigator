package space.taran.arknavigator.mvp.presenter.adapter

import space.taran.arknavigator.ui.fragments.utils.Preview
import space.taran.arknavigator.mvp.view.item.FileItemView
import space.taran.arknavigator.utils.findLongestCommonPrefix
import java.nio.file.Path

class FoldersWalker(paths: List<Path>, onClick: ItemClickHandler<Path>)
    : ItemsReversiblePresenter<Path, Path, FileItemView>(
        findLongestCommonPrefix(paths), paths, onClick) {

    override fun bindView(view: FileItemView) {
        val path = items()[view.position()]
        view.setText(path.fileName.toString())

        view.setIcon(Preview.provide(path))
    }
}