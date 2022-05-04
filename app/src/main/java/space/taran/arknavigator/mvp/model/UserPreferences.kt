package space.taran.arknavigator.mvp.model

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import space.taran.arknavigator.BuildConfig
import space.taran.arknavigator.utils.Sorting
import javax.inject.Inject

class UserPreferences @Inject constructor(val context: Context) {
    private val SHARED_PREFERENCES_KEY = "user_preferences"

    private val Context.preferencesDatastore by preferencesDataStore(
        SHARED_PREFERENCES_KEY
    )

    private val dataStore = context.preferencesDatastore

    val sortingFlow: Flow<Sorting> = dataStore.data.map { preferences ->
        convertIntToSorting(preferences[PreferencesKeys.SORTING])
    }.distinctUntilChanged()

    val ascendingFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SORTING_ASCENDING]
            ?: DefaultValues.SORTING_ASCENDING
    }.distinctUntilChanged()

    suspend fun clearPreferences() {
        setSorting(DefaultValues.SORTING)
        setSortingAscending(DefaultValues.SORTING_ASCENDING)
        setCrashReportEnabled(DefaultValues.CRASH_REPORT)
        setCacheReplicationEnabled(DefaultValues.IMG_CACHE_REPLICATION)
        setIndexReplicationEnabled(DefaultValues.INDEX_REPLICATION)
        setRemovingLostResourcesTagsEnabled(
            DefaultValues.REMOVING_LOST_RESOURCES_TAGS
        )
    }

    suspend fun setSorting(sorting: Sorting) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SORTING] = sorting.ordinal
        }
    }

    suspend fun getSorting(): Sorting {
        val intValue = dataStore.data.first()[PreferencesKeys.SORTING]
        return convertIntToSorting(intValue)
    }

    suspend fun setSortingAscending(isAscending: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SORTING_ASCENDING] = isAscending
        }
    }

    suspend fun isSortingAscending(): Boolean =
        dataStore.data.first()[PreferencesKeys.SORTING_ASCENDING]
            ?: DefaultValues.SORTING_ASCENDING

    suspend fun setCrashReportEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CRASH_REPORT] = enabled
        }
    }

    suspend fun isKindTagsEnabled(): Boolean =
        dataStore.data.first()[
            PreferencesKeys.SHOW_KINDS
        ] ?: DefaultValues.SHOW_KIND

    suspend fun setKindTagsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_KINDS] = enabled
        }
    }

    suspend fun isCrashReportEnabled(): Boolean =
        dataStore.data.first()[PreferencesKeys.CRASH_REPORT]
            ?: DefaultValues.CRASH_REPORT

    suspend fun setCacheReplicationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IMG_CACHE_REPLICATION] = enabled
        }
    }

    suspend fun isCacheReplicationEnabled(): Boolean =
        dataStore.data.first()[PreferencesKeys.IMG_CACHE_REPLICATION]
            ?: DefaultValues.IMG_CACHE_REPLICATION

    suspend fun setIndexReplicationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.INDEX_REPLICATION] = enabled
        }
    }

    suspend fun isIndexReplicationEnabled(): Boolean =
        dataStore.data.first()[PreferencesKeys.INDEX_REPLICATION]
            ?: DefaultValues.INDEX_REPLICATION

    suspend fun setRemovingLostResourcesTagsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMOVING_LOST_RESOURCES_TAGS] = enabled
        }
    }

    suspend fun isRemovingLostResourcesTagsEnabled(): Boolean =
        dataStore.data.first()[PreferencesKeys.REMOVING_LOST_RESOURCES_TAGS]
            ?: DefaultValues.REMOVING_LOST_RESOURCES_TAGS

    suspend fun isFirstOpen(): Boolean =
        dataStore.data.first()[PreferencesKeys.IS_FIRST_OPEN]
            ?: let {
                dataStore.edit { pref ->
                    pref[PreferencesKeys.IS_FIRST_OPEN] = false
                }
                DefaultValues.IS_FIRST_OPEN
            }

    private fun convertIntToSorting(intValue: Int?): Sorting {
        return if (intValue == null) Sorting.DEFAULT
        else Sorting.values()[intValue]
    }

    object PreferencesKeys {
        val SORTING = intPreferencesKey("sorting")
        val SORTING_ASCENDING = booleanPreferencesKey("sorting_is_ascending")
        val CRASH_REPORT =
            booleanPreferencesKey("crash_report")
        val IMG_CACHE_REPLICATION =
            booleanPreferencesKey("img_cache_replication")
        val INDEX_REPLICATION =
            booleanPreferencesKey("index_replication")
        val REMOVING_LOST_RESOURCES_TAGS =
            booleanPreferencesKey("removing_lost_resources_tags")
        val SHOW_KINDS = booleanPreferencesKey("show_kind_preference")
        val IS_FIRST_OPEN = booleanPreferencesKey("is_first_open")
    }

    private object DefaultValues {
        val SORTING = Sorting.DEFAULT
        val SORTING_ASCENDING = true
        val CRASH_REPORT = BuildConfig.DEBUG
        val IMG_CACHE_REPLICATION = false
        val INDEX_REPLICATION = false
        val REMOVING_LOST_RESOURCES_TAGS = false
        val SHOW_KIND = false
        val IS_FIRST_OPEN = true
    }
}
