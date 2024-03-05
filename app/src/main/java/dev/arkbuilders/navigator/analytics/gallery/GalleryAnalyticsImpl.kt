package dev.arkbuilders.navigator.analytics.gallery

import dev.arkbuilders.navigator.analytics.trackEvent
import dev.arkbuilders.navigator.analytics.trackScreen
import org.matomo.sdk.Tracker

class GalleryAnalyticsImpl(
    private val matomoTracker: Tracker
): GalleryAnalytics {
    override fun trackScreen() = matomoTracker.trackScreen {
        screen(SCREEN_NAME)
    }

    override fun trackResOpen() = matomoTracker.trackEvent {
        event(SCREEN_NAME, "Resource open")
    }

    override fun trackResShare() = matomoTracker.trackEvent {
        event(SCREEN_NAME, "Resource share")
    }

    override fun trackResInfo() = matomoTracker.trackEvent {
        event(SCREEN_NAME, "Resource info")
    }

    override fun trackResEdit() = matomoTracker.trackEvent {
        event(SCREEN_NAME, "Resource edit")
    }

    override fun trackResRemove() = matomoTracker.trackEvent {
        event(SCREEN_NAME, "Resource remove")
    }

    override fun trackTagSelect() = matomoTracker.trackEvent {
        event(SCREEN_NAME, "Tag select")
    }

    override fun trackTagRemove() = matomoTracker.trackEvent {
        event(SCREEN_NAME, "Tag remove")
    }

    override fun trackTagsEdit() = matomoTracker.trackEvent {
        event(SCREEN_NAME, "Tags edit")
    }

    companion object {
        private const val SCREEN_NAME = "Gallery screen"
    }
}
