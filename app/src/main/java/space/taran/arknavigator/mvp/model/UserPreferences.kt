package space.taran.arknavigator.mvp.model

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import space.taran.arknavigator.utils.Sorting
import javax.inject.Inject

class UserPreferences @Inject constructor(val context: Context) {
    private val SHARED_PREFERENCES_KEY = "user_preferences"

    private val Context.preferencesDatastore by preferencesDataStore((SHARED_PREFERENCES_KEY))

    private val dataStore = context.preferencesDatastore

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
        dataStore.edit { preferences -> preferences[PreferencesKeys.SORTING_ORDER] = isAscending }
    }

    suspend fun isSortingAscending(): Boolean =
        dataStore.data.first()[PreferencesKeys.SORTING_ORDER] ?: true

    private fun convertIntToSorting(intValue: Int?): Sorting {
        return if (intValue == null) Sorting.DEFAULT
        else Sorting.values()[intValue]
    }

    private object PreferencesKeys {
        val SORTING_PREFERENCE = intPreferencesKey("sorting_preference")
        val SORTING_ORDER = booleanPreferencesKey("sorting_preference_is_ascending")
    }
}