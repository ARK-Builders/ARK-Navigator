package dev.arkbuilders.navigator.analytics.resources

import dev.arkbuilders.arklib.data.storage.StorageException
import dev.arkbuilders.components.tagselector.QueryMode
import dev.arkbuilders.components.tagselector.TagsSorting
import dev.arkbuilders.navigator.data.utils.Sorting

interface ResourcesAnalytics {
    fun trackScreen()
    fun trackResClick()
    fun trackMoveSelectedRes()
    fun trackCopySelectedRes()
    fun trackRemoveSelectedRes()
    fun trackShareSelectedRes()
    fun trackResShuffle()
    fun trackTagSortCriteria(tagsSorting: TagsSorting)
    fun trackResSortCriteria(sorting: Sorting)
    fun trackQueryModeChanged(queryMode: QueryMode)
    fun trackStorageProvideException(exception: StorageException)
}
