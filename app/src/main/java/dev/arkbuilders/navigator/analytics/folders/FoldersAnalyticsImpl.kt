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

    override fun trackRootOpen() = matomoTracker.trackScreenEvent("Root opened")

    override fun trackFavOpen() = matomoTracker.trackScreenEvent("Fav opened")

    override fun trackRootAdded() = matomoTracker.trackScreenEvent("Root added")

    override fun trackFavAdded() = matomoTracker.trackScreenEvent("Fav added")

    private fun Tracker.trackScreenEvent(action: String) = this.trackEvent {
        event(SCREEN_NAME, action)
    }

    companion object {
        private const val SCREEN_NAME = "Folders screen"
    }
}
