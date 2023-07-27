package dev.arkbuilders.navigator.mvp.view.item

import kotlinx.coroutines.CoroutineScope
import space.taran.arklib.ResourceId
import space.taran.arklib.domain.meta.Metadata
import space.taran.arklib.domain.preview.PreviewLocator
import dev.arkbuilders.navigator.utils.Score
import java.nio.file.Path

interface FileItemView {
    fun position(): Int

    fun setFolderIcon()

    fun setGenericIcon(path: Path)

    fun reset(isSelectingEnabled: Boolean, isItemSelected: Boolean)
    fun setSelected(isItemSelected: Boolean)

    fun setThumbnail(
        path: Path,
        id: ResourceId,
        meta: Metadata,
        locator: PreviewLocator,
        presenterScope: CoroutineScope
    )

    fun setText(title: String, shortName: Boolean = false)

    fun setPinned(isPinned: Boolean)

    fun displayScore(sortByScoresEnabled: Boolean, score: Score)
}
