package dev.arkbuilders.navigator.analytics.resources

import dev.arkbuilders.arklib.data.storage.StorageException
import dev.arkbuilders.components.tagselector.QueryMode
import dev.arkbuilders.components.tagselector.TagsSorting
import dev.arkbuilders.navigator.analytics.trackEvent
import dev.arkbuilders.navigator.analytics.trackScreen
import dev.arkbuilders.navigator.data.utils.Sorting
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper

class ResourcesAnalyticsImpl(
    private val matomoTracker: Tracker
) : ResourcesAnalytics {
    override fun trackScreen() = matomoTracker.trackScreen { screen(SCREEN_NAME) }

    override fun trackResClick() =
        matomoTracker.trackScreenEvent("Resource Click")

    override fun trackMoveSelectedRes() =
        matomoTracker.trackScreenEvent("Move selected resources")

    override fun trackCopySelectedRes() =
        matomoTracker.trackScreenEvent("Copy selected resources")

    override fun trackRemoveSelectedRes() =
        matomoTracker.trackScreenEvent("Remove selected resources")

    override fun trackShareSelectedRes() =
        matomoTracker.trackScreenEvent("Share selected resources")

    override fun trackResShuffle() =
        matomoTracker.trackScreenEvent("Shuffle resources")

    override fun trackTagSortCriteria(tagsSorting: TagsSorting) =
        matomoTracker.trackScreenEvent("Tag sorting criteria: ${tagsSorting.name}")

    override fun trackResSortCriteria(sorting: Sorting) =
        matomoTracker.trackScreenEvent("Resources sorting criteria: ${sorting.name}")

    override fun trackQueryModeChanged(queryMode: QueryMode) =
        matomoTracker.trackScreenEvent("Query mode: ${queryMode.name}")

    override fun trackStorageProvideException(exception: StorageException) =
        TrackHelper
            .track()
            .exception(exception)
            .description("Storage provide")
            .fatal(false)
            .with(matomoTracker)

    private fun Tracker.trackScreenEvent(action: String) = this.trackEvent {
        event(SCREEN_NAME, action)
    }

    companion object {
        private const val SCREEN_NAME = "Resources screen"
    }
}
