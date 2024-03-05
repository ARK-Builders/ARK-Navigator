package dev.arkbuilders.navigator.analytics.folders

import android.content.Context
import dev.arkbuilders.navigator.analytics.trackEvent
import dev.arkbuilders.navigator.analytics.trackScreen
import org.matomo.sdk.Tracker
import javax.inject.Inject

class FoldersAnalyticsImpl @Inject constructor(
    private val matomoTracker: Tracker,
    private val context: Context
) : FoldersAnalytics {

    override fun trackScreen() = matomoTracker
        .trackScreen { screen(SCREEN_NAME) }

    override fun trackRootOpen() = matomoTracker.trackEvent {
        event(SCREEN_NAME, "Root opened")
    }

    override fun trackFavOpen() = matomoTracker.trackEvent {
        event(SCREEN_NAME, "Fav opened")
    }

    override fun trackRootAdded() = matomoTracker.trackEvent {
        event(SCREEN_NAME, "Root added")
    }

    override fun trackFavAdded() = matomoTracker.trackEvent {
        event(SCREEN_NAME, "Fav added")
    }

    companion object {
        private const val SCREEN_NAME = "Folders screen"
    }
}
