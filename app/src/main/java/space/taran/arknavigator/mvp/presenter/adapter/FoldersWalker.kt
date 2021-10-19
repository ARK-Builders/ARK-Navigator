package space.taran.arknavigator.mvp.presenter.adapter

import space.taran.arknavigator.mvp.model.repo.PreviewsRepo
import space.taran.arknavigator.mvp.view.item.FileItemView
import space.taran.arknavigator.utils.findLongestCommonPrefix
import java.nio.file.Path
import javax.inject.Inject

class FoldersWalker(paths: List<Path>, onClick: ItemClickHandler<Path>)
    : ItemsReversiblePresenter<Path, Path, FileItemView>(
        findLongestCommonPrefix(paths), paths, onClick) {

    @Inject
    lateinit var previewsRepo: PreviewsRepo

    override fun bindView(view: FileItemView) {
        val path = items()[view.position()]
        view.setText(path.fileName.toString())

        view.setIcon(previewsRepo.providePreview(path))
    }
}