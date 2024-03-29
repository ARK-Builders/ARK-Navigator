package dev.arkbuilders.navigator.presentation.screen.gallery

import dev.arkbuilders.navigator.presentation.common.CommonMvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType
import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.data.index.Resource
import dev.arkbuilders.arklib.data.meta.Metadata
import dev.arkbuilders.arklib.user.tags.Tags
import java.nio.file.Path

@StateStrategyType(AddToEndSingleStrategy::class)
interface GalleryView : CommonMvpView {
    fun init()
    fun updatePagerAdapter()
    fun updatePagerAdapterWithDiff()
    fun setControlsVisibility(visible: Boolean)
    fun exitFullscreen()
    fun setPreviewsScrollingEnabled(enabled: Boolean)
    fun setupPreview(pos: Int, meta: Metadata)
    fun displayPreviewTags(resource: ResourceId, tags: Tags)
    fun setProgressVisibility(isVisible: Boolean, withText: String = "")
    fun displaySelected(
        selected: Boolean,
        showAnim: Boolean,
        selectedCount: Int,
        itemCount: Int
    )

    @StateStrategyType(SkipStrategy::class)
    fun openLink(link: String)

    @StateStrategyType(SkipStrategy::class)
    fun shareLink(link: String)

    @StateStrategyType(SkipStrategy::class)
    fun showInfoAlert(path: Path, resource: Resource, metadata: Metadata)

    @StateStrategyType(SkipStrategy::class)
    fun viewInExternalApp(resourcePath: Path)

    @StateStrategyType(SkipStrategy::class)
    fun editResource(resourcePath: Path)

    @StateStrategyType(SkipStrategy::class)
    fun shareResource(resourcePath: Path)

    @StateStrategyType(SkipStrategy::class)
    fun showEditTagsDialog(resource: ResourceId)

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

    @StateStrategyType(SkipStrategy::class)
    fun displayStorageException(label: String, msg: String)
}
