package space.taran.arknavigator.mvp.model.repo.preferences

import kotlinx.coroutines.flow.Flow

interface Preferences {
    suspend fun <T> get(key: PreferenceKey<T>): T
    suspend fun <T> set(key: PreferenceKey<T>, value: T)
    suspend fun <T> flow(key: PreferenceKey<T>): Flow<T>

    suspend fun clearPreferences() {
        val preferencesToReset = listOf(
            PreferenceKey.Sorting,
            PreferenceKey.IsSortingAscending,
            PreferenceKey.CrashReport,
            PreferenceKey.ImgCacheReplication,
            PreferenceKey.IndexReplication,
            PreferenceKey.RemovingLostResourcesTags,
            PreferenceKey.BackupEnabled,
            PreferenceKey.ShortFileNames
        )

        preferencesToReset.forEach {
            set(it, it.defaultValue)
        }
    }
}

sealed class PreferenceKey<out T>(val defaultValue: T) {
    object Sorting : PreferenceKey<Int>(0)
    object IsSortingAscending : PreferenceKey<Boolean>(true)
    object CrashReport : PreferenceKey<Boolean>(true)
    object ImgCacheReplication : PreferenceKey<Boolean>(false)
    object IndexReplication : PreferenceKey<Boolean>(false)
    object RemovingLostResourcesTags : PreferenceKey<Boolean>(false)
    object ShowKinds : PreferenceKey<Boolean>(false)
    object WasRootsScanShown : PreferenceKey<Boolean>(false)
    object BackupEnabled : PreferenceKey<Boolean>(true)
    object ShortFileNames : PreferenceKey<Boolean>(true)
}
