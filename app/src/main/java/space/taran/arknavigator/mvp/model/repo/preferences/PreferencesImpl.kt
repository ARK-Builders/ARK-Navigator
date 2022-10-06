package space.taran.arknavigator.mvp.model.repo.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import space.taran.arknavigator.mvp.model.repo.preferences.Preferences as ArkPreferences

class PreferencesImpl @Inject constructor(val context: Context) : ArkPreferences {
    private val SHARED_PREFERENCES_KEY = "user_preferences"

    private val Context.preferencesDatastore by preferencesDataStore(
        SHARED_PREFERENCES_KEY
    )

    private val dataStore = context.preferencesDatastore

    override suspend fun <T> get(key: PreferenceKey<T>): T {
        val prefKey = resolveKey(key)
        return dataStore.data.first()[prefKey] ?: key.defaultValue
    }

    override suspend fun <T> set(key: PreferenceKey<T>, value: T) {
        dataStore.edit { pref ->
            val prefKey = resolveKey(key)
            pref[prefKey] = value
        }
    }

    override suspend fun <T> flow(key: PreferenceKey<T>) =
        dataStore.data.map { pref ->
            val prefKey = resolveKey(key)
            pref[prefKey] ?: key.defaultValue
        }

    private fun <T> resolveKey(key: PreferenceKey<T>): Preferences.Key<T> {
        val result = when (key) {
            PreferenceKey.Sorting -> intPreferencesKey("sorting")
            PreferenceKey.IsSortingAscending ->
                booleanPreferencesKey("sorting_is_ascending")
            PreferenceKey.TagsSorting -> intPreferencesKey("tags_sorting")
            PreferenceKey.TagsSortingAscending ->
                booleanPreferencesKey("tags_sorting_ascending")
            PreferenceKey.CrashReport ->
                booleanPreferencesKey("crash_report")
            PreferenceKey.ImgCacheReplication ->
                booleanPreferencesKey("img_cache_replication")
            PreferenceKey.IndexReplication ->
                booleanPreferencesKey("index_replication")
            PreferenceKey.RemovingLostResourcesTags ->
                booleanPreferencesKey("removing_lost_resources_tags")
            PreferenceKey.ShowKinds ->
                booleanPreferencesKey("show_kind_preference")
            PreferenceKey.WasRootsScanShown ->
                booleanPreferencesKey("was_roots_scan_shown")
            PreferenceKey.BackupEnabled ->
                booleanPreferencesKey("backup")
            PreferenceKey.ShortFileNames ->
                booleanPreferencesKey("short_file_names")
        }

        return result as Preferences.Key<T>
    }
}
