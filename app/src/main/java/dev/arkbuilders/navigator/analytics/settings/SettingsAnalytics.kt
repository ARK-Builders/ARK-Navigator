package dev.arkbuilders.navigator.analytics.settings

interface SettingsAnalytics {
    fun trackScreen()
    fun trackBooleanPref(name: String, enabled: Boolean)
}
