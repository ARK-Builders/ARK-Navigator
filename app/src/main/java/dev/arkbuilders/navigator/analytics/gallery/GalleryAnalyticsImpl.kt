package dev.arkbuilders.navigator.analytics.gallery

import dev.arkbuilders.navigator.analytics.trackEvent
import dev.arkbuilders.navigator.analytics.trackScreen
import org.matomo.sdk.Tracker

class GalleryAnalyticsImpl(
    private val matomoTracker: Tracker
) : GalleryAnalytics {
    override fun trackScreen() = matomoTracker.trackScreen {
        screen(SCREEN_NAME)
    }

    override fun trackResOpen() = matomoTracker.trackScreenEvent("Resource open")

    override fun trackResShare() = matomoTracker.trackScreenEvent("Resource share")

    override fun trackResInfo() = matomoTracker.trackScreenEvent("Resource info")

    override fun trackResEdit() = matomoTracker.trackScreenEvent("Resource edit")

    override fun trackResRemove() = matomoTracker.trackScreenEvent("Resource remove")

    override fun trackTagSelect() = matomoTracker.trackScreenEvent("Tag select")

    override fun trackTagRemove() = matomoTracker.trackScreenEvent("Tag remove")

    override fun trackTagsEdit() = matomoTracker.trackScreenEvent("Tags edit")

    private fun Tracker.trackScreenEvent(action: String) = this.trackEvent {
        event(SCREEN_NAME, action)
    }

    companion object {
        private const val SCREEN_NAME = "Gallery screen"
    }
}
