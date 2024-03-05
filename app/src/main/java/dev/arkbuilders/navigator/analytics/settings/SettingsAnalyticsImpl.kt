package dev.arkbuilders.navigator.analytics.settings

import dev.arkbuilders.navigator.analytics.trackEvent
import dev.arkbuilders.navigator.analytics.trackScreen
import org.matomo.sdk.Tracker

class SettingsAnalyticsImpl(
    private val matomoTracker: Tracker
): SettingsAnalytics {
    override fun trackScreen() = matomoTracker.trackScreen { screen(SCREEN_NAME) }

    override fun trackBooleanPref(name: String, enabled: Boolean) {
        val enabledStr = if (enabled) "enabled" else "disabled"
        matomoTracker
            .trackEvent { event(SCREEN_NAME, "$name is $enabledStr") }
    }

    companion object {
        private const val SCREEN_NAME = "Settings screen"
    }

}
