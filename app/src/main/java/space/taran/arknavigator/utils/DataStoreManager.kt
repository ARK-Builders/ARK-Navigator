package space.taran.arknavigator.utils

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject

const val SHARED_PREFERENCES_KEY = "user_preferences"

private val Context.preferencesDatastore by preferencesDataStore((SHARED_PREFERENCES_KEY))

class DataStoreManager @Inject constructor(val context: Context){
    private val dataStore = context.preferencesDatastore

    suspend fun setUserSorting(sortMode: Int){
        dataStore.edit { preferences -> preferences[PreferencesKeys.SORTING_PREFERENCE] = sortMode}
    }

    suspend fun getUserSorting(): Int =
        dataStore.data.first()[PreferencesKeys.SORTING_PREFERENCE] ?: 0

    suspend fun setUserSortOrder(isAscending: Boolean){
        dataStore.edit { preferences -> preferences[PreferencesKeys.SORTING_ORDER] = isAscending}
    }

    suspend fun getUserSortOrder(): Boolean =
        dataStore.data.first()[PreferencesKeys.SORTING_ORDER] ?: false

    private object PreferencesKeys {
        val SORTING_PREFERENCE = intPreferencesKey("sorting_preference")
        val SORTING_ORDER = booleanPreferencesKey("sorting_preference_is_ascending")
    }
}