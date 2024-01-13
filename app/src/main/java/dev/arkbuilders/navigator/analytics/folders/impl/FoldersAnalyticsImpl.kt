package dev.arkbuilders.navigator.analytics.folders.impl

import android.content.Context
import dev.arkbuilders.navigator.R
import dev.arkbuilders.navigator.analytics.folders.FoldersAnalytics
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper
import javax.inject.Inject

class FoldersAnalyticsImpl @Inject constructor(
    private val matomoTracker: Tracker,
    private val context: Context
) : FoldersAnalytics {

    override fun trackScreen() {
        TrackHelper.track().screen(
            context.getString(R.string.folders_screen_state)).with(matomoTracker)
    }

    override fun trackAction() {
        TrackHelper.track().event(
            context.getString(R.string.folders_screen_state),
            context.getString(R.string.folders_screen_state)
        )
    }
}
