package space.taran.arknavigator.mvp.view

import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.utils.Score
import space.taran.arknavigator.utils.Tags
import java.nio.file.Path

@StateStrategyType(AddToEndSingleStrategy::class)
interface GalleryView : CommonMvpView {
    fun init()
    fun updatePagerAdapter()
    fun updatePagerAdapterWithDiff()
    fun setControlsVisibility(visible: Boolean)
    fun exitFullscreen()
    fun setPreviewsScrollingEnabled(enabled: Boolean)
    fun setupPreview(pos: Int, resource: ResourceMeta, filePath: String)
    fun displayPreviewTags(resource: ResourceId, tags: Tags)
    fun setProgressVisibility(isVisible: Boolean, withText: String = "")
    fun displayScore(score: Score)
    fun displaySelected(
        selected: Boolean,
        showAnim: Boolean,
        selectedCount: Int,
        itemCount: Int
    )

    fun setScoringControlsVisibility(isVisible: Boolean)

    @StateStrategyType(SkipStrategy::class)
    fun openLink(link: String)

    @StateStrategyType(SkipStrategy::class)
    fun shareLink(link: String)

    @StateStrategyType(SkipStrategy::class)
    fun showInfoAlert(path: Path, resourceMeta: ResourceMeta)

    @StateStrategyType(SkipStrategy::class)
    fun viewInExternalApp(resourcePath: Path)

    @StateStrategyType(SkipStrategy::class)
    fun editResource(resourcePath: Path)

    @StateStrategyType(SkipStrategy::class)
    fun shareResource(resourcePath: Path)

    @StateStrategyType(SkipStrategy::class)
    fun showEditTagsDialog(resource: Long)

    @StateStrategyType(SkipStrategy::class)
    fun deleteResource(pos: Int)

    @StateStrategyType(SkipStrategy::class)
    fun toggleSelecting(enabled: Boolean)

    @StateStrategyType(SkipStrategy::class)
    fun notifyResourcesChanged()

    @StateStrategyType(SkipStrategy::class)
    fun notifyTagsChanged()

    @StateStrategyType(SkipStrategy::class)
    fun notifyCurrentItemChanged()

    @StateStrategyType(SkipStrategy::class)
    fun notifyResourceScoresChanged()

    @StateStrategyType(SkipStrategy::class)
    fun notifySelectedChanged(selected: List<ResourceId>)
}
