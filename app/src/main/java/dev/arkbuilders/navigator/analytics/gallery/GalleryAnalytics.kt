package dev.arkbuilders.navigator.analytics.gallery

interface GalleryAnalytics {
    fun trackScreen()
    fun trackResOpen()
    fun trackResShare()
    fun trackResInfo()
    fun trackResEdit()
    fun trackResRemove()
    fun trackTagSelect()
    fun trackTagRemove()
    fun trackTagsEdit()
}
