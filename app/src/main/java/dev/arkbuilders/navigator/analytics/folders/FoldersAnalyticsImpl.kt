package dev.arkbuilders.navigator.analytics.folders

import org.matomo.sdk.Tracker
import javax.inject.Inject

class FoldersAnalyticsImpl @Inject constructor(
    private val matomoTracker: Tracker
): FoldersAnalytics {

    override fun trackScreen() {
        TODO("Not yet implemented")
    }

    override fun trackAction() {
        TODO("Not yet implemented")
    }
}
