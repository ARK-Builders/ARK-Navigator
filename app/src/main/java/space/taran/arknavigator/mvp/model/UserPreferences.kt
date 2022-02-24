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
        convertIntToSorting(preferences[PreferencesKeys.SORTING_PREFERENCE])
    }.distinctUntilChanged()

    val ascendingFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SORTING_ORDER] ?: true
    }.distinctUntilChanged()

    suspend fun clearPreferences() {
        setSorting(Sorting.DEFAULT)
        setSortingAscending(IS_SORTING_ASCENDING_DEFAULT)
        setCrashReportEnabled(null)
        setCacheReplicationEnabled(CACHE_REPLICATION_DEFAULT)
        setIndexReplicationEnabled(INDEX_REPLICATION_DEFAULT)
    }

    suspend fun setSorting(sorting: Sorting) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SORTING_PREFERENCE] = sorting.ordinal
        }
    }

    suspend fun getSorting(): Sorting {
        val intValue = dataStore.data.first()[PreferencesKeys.SORTING_PREFERENCE]
        return convertIntToSorting(intValue)
    }

    suspend fun setSortingAscending(isAscending: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SORTING_ORDER] = isAscending
        }
    }

    suspend fun isSortingAscending(): Boolean =
        dataStore.data.first()[
            PreferencesKeys.SORTING_ORDER
        ] ?: IS_SORTING_ASCENDING_DEFAULT

    private fun convertIntToSorting(intValue: Int?): Sorting {
        return if (intValue == null) Sorting.DEFAULT
        else Sorting.values()[intValue]
    }

    suspend fun setCrashReportEnabled(enabled: Boolean?) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CRASH_REPORT_PREFERENCE] =
                enabled ?: BuildConfig.DEBUG
        }
    }

    suspend fun setKindTagsEnabled(enabled: Boolean?) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_KIND_PREFERENCE] =
                enabled ?: BuildConfig.DEBUG
        }
    }

    suspend fun isCrashReportEnabled(): Boolean =
        dataStore.data.first()[PreferencesKeys.CRASH_REPORT_PREFERENCE]
            ?: BuildConfig.DEBUG

    suspend fun setCacheReplicationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IMG_CACHE_REPLICATION_PREF] = enabled
        }
    }

    suspend fun isCacheReplicationEnabled(): Boolean =
        dataStore.data.first()[PreferencesKeys.IMG_CACHE_REPLICATION_PREF]
            ?: CACHE_REPLICATION_DEFAULT

    suspend fun setIndexReplicationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.INDEX_REPLICATION_PREF] = enabled
        }
    }

    suspend fun isIndexReplicationEnabled(): Boolean =
        dataStore.data.first()[PreferencesKeys.INDEX_REPLICATION_PREF]
            ?: INDEX_REPLICATION_DEFAULT

    suspend fun isShowTagsEnabled(): Boolean =
        dataStore.data.first()[
            PreferencesKeys.SHOW_KIND_PREFERENCE
        ] ?: SHOW_KIND_PREFERENCE

    private object PreferencesKeys {
        val SORTING_PREFERENCE = intPreferencesKey("sorting_preference")
        val SORTING_ORDER = booleanPreferencesKey("sorting_preference_is_ascending")
        val CRASH_REPORT_PREFERENCE =
            booleanPreferencesKey("crash_report_preference")
        val SHOW_KIND_PREFERENCE =
            booleanPreferencesKey("show_kind_preference")
        val IMG_CACHE_REPLICATION_PREF =
            booleanPreferencesKey("img_cache_replication_preference")
        val INDEX_REPLICATION_PREF =
            booleanPreferencesKey("index_replication_preference")
    }
}

const val IS_SORTING_ASCENDING_DEFAULT = true
const val CACHE_REPLICATION_DEFAULT = false
const val INDEX_REPLICATION_DEFAULT = false
const val SHOW_KIND_PREFERENCE = false
