package space.taran.arknavigator.mvp.view.item

import space.taran.arklib.domain.index.Resource
import space.taran.arklib.domain.preview.PreviewAndThumbnail
import space.taran.arknavigator.utils.Score
import java.nio.file.Path

interface FileItemView {
    fun position(): Int

    fun setFolderIcon()

    fun setGenericIcon(path: Path)

    fun reset(isSelectingEnabled: Boolean, isItemSelected: Boolean)
    fun setSelected(isItemSelected: Boolean)

    fun setIconOrPreview(
        path: Path,
        resource: Resource,
        previewAndThumbnail: PreviewAndThumbnail?
    )

    fun setText(title: String, shortName: Boolean = false)

    fun setPinned(isPinned: Boolean)

    fun displayScore(sortByScoresEnabled: Boolean, score: Score)
}
