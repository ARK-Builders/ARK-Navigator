package dev.arkbuilders.navigator.presentation.screen.resources

import dev.arkbuilders.components.tagselector.QueryMode
import dev.arkbuilders.navigator.presentation.common.CommonMvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType
import java.nio.file.Path

@StateStrategyType(AddToEndSingleStrategy::class)
interface ResourcesView : CommonMvpView {
    fun init(ascending: Boolean, sortByScoresEnabled: Boolean)
    fun initResourcesAdapter()
    fun updateResourcesAdapter()
    fun setProgressVisibility(isVisible: Boolean, withText: String = "")
    fun setToolbarTitle(title: String)
    fun updateMenu(queryMode: QueryMode)
    fun updateOrderBtn(isAscending: Boolean)
    fun setSelectingEnabled(enabled: Boolean)
    fun setSelectingCount(selected: Int, all: Int)
    fun setPreviewGenerationProgress(isVisible: Boolean)
    fun setMetadataExtractionProgress(isVisible: Boolean)

    @StateStrategyType(SkipStrategy::class)
    fun toastResourcesSelected(selected: Int)

    @StateStrategyType(SkipStrategy::class)
    fun toastResourcesSelectedFocusMode(selected: Int, hidden: Int)

    @StateStrategyType(SkipStrategy::class)
    fun toastPathsFailed(failedPaths: List<Path>)

    @StateStrategyType(SkipStrategy::class)
    fun onSelectingChanged(enabled: Boolean)

    @StateStrategyType(SkipStrategy::class)
    fun clearStackedToasts()

    @StateStrategyType(SkipStrategy::class)
    fun shareResources(resources: List<Path>)

    @StateStrategyType(SkipStrategy::class)
    fun displayStorageException(label: String, msg: String)
}
