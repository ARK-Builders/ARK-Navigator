package dev.arkbuilders.navigator.analytics.folders

interface FoldersAnalytics {
    fun trackScreen()
    fun trackRootOpen()
    fun trackFavOpen()
    fun trackRootAdded()
    fun trackFavAdded()
}
