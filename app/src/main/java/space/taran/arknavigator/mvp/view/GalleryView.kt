package space.taran.arknavigator.mvp.view

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.utils.Tags
import java.nio.file.Path

@StateStrategyType(AddToEndSingleStrategy::class)
interface GalleryView: MvpView {
    fun init()
    fun updatePagerAdapter()
    fun setControlsVisibility(visible: Boolean)
    fun exitFullscreen()
    fun setPreviewsScrollingEnabled(enabled: Boolean)
    fun setupPreview(pos: Int, resource: ResourceMeta, filePath: String)
    fun displayPreviewTags(resource: ResourceId, tags: Tags)
    fun setProgressVisibility(isVisible: Boolean, withText: String = "")

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
    fun notifyResourcesChanged()
    @StateStrategyType(SkipStrategy::class)
    fun notifyTagsChanged()
}
