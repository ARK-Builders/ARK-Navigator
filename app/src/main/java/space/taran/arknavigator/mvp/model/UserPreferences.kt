package space.taran.arknavigator.mvp.model

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import space.taran.arknavigator.utils.Sorting
import space.taran.arknavigator.utils.extensions.convertToSorting
import javax.inject.Inject

class UserPreferences @Inject constructor(val context: Context) {
    private val SHARED_PREFERENCES_KEY = "user_preferences"

    private val Context.preferencesDatastore by preferencesDataStore((SHARED_PREFERENCES_KEY))

    private val dataStore = context.preferencesDatastore

    suspend fun setUserSorting(sorting: Sorting) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SORTING_PREFERENCE] = sorting.ordinal
        }
    }

    suspend fun getUserSorting(): Sorting =
        dataStore.data.first()[PreferencesKeys.SORTING_PREFERENCE]
            ?.convertToSorting()
            ?: Sorting.DEFAULT

    suspend fun setUserSortAscending(isAscending: Boolean) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.SORTING_ORDER] = isAscending }
    }

    suspend fun isUserSortAscending(): Boolean =
        dataStore.data.first()[PreferencesKeys.SORTING_ORDER] ?: true

    private object PreferencesKeys {
        val SORTING_PREFERENCE = intPreferencesKey("sorting_preference")
        val SORTING_ORDER = booleanPreferencesKey("sorting_preference_is_ascending")
    }
}