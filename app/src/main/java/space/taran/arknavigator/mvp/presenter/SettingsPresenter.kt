package space.taran.arknavigator.mvp.presenter

import android.util.Log
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import space.taran.arknavigator.mvp.model.repo.preferences.PreferenceKey
import space.taran.arknavigator.mvp.model.repo.preferences.Preferences
import space.taran.arknavigator.mvp.view.SettingsView
import space.taran.arknavigator.navigation.AppRouter
import space.taran.arknavigator.utils.LogTags.SETTINGS_SCREEN
import javax.inject.Inject

class SettingsPresenter : MvpPresenter<SettingsView>() {

    @Inject
    lateinit var router: AppRouter

    @Inject
    lateinit var preferences: Preferences

    override fun onFirstViewAttach() {
        Log.d(SETTINGS_SCREEN, "first view attached in SettingsPresenter")
        super.onFirstViewAttach()

        viewState.init()
        presenterScope.launch {
            notifyAllPreferences()
        }
    }

    fun onCrashReportingClick(
        enabled: Boolean
    ) {
        presenterScope.launch {
            viewState.toastCrashReportingEnabled(enabled)
            Log.d(
                SETTINGS_SCREEN,
                "Saving crash report preference, is enabled: $enabled"
            )
            preferences.set(PreferenceKey.CrashReport, enabled)
            notifyCrashReportPref()
        }
    }

    fun onImgCacheReplicationClick(enabled: Boolean) {
        presenterScope.launch {
            viewState.toastImageCacheReplicationEnabled(enabled)
            Log.d(
                SETTINGS_SCREEN,
                "Saving imgCacheReplication preference, " +
                    "is enabled: $enabled"
            )
            preferences.set(PreferenceKey.ImgCacheReplication, enabled)
            notifyImgCacheReplicationPref()
        }
    }

    fun onIndexReplicationClick(enabled: Boolean) {
        presenterScope.launch {
            viewState.toastIndexReplicationEnabled(enabled)
            Log.d(
                SETTINGS_SCREEN,
                "Saving indexReplication preference, " +
                    "is enabled: $enabled"
            )
            preferences.set(PreferenceKey.IndexReplication, enabled)
            notifyIndexReplicationPref()
        }
    }

    fun onRemovingLostResourcesTagsClick(checked: Boolean) {
        presenterScope.launch {
            viewState.toastRemovingTagsEnabled(checked)
            preferences.set(PreferenceKey.RemovingLostResourcesTags, checked)
            notifyRemovingLostResourcesTags()
        }
    }

    fun onBackupClick(checked: Boolean) = presenterScope.launch {
        viewState.toastBackup(checked)
        preferences.set(PreferenceKey.BackupEnabled, checked)
        notifyBackupPref()
    }

    fun onResetPreferencesClick() {
        presenterScope.launch {
            preferences.clearPreferences()
            notifyAllPreferences()
        }
    }

    private suspend fun notifyAllPreferences() {
        notifyCrashReportPref()
        notifyImgCacheReplicationPref()
        notifyIndexReplicationPref()
        notifyRemovingLostResourcesTags()
        notifyBackupPref()
    }

    private suspend fun notifyCrashReportPref() =
        viewState.setCrashReportPreference(
            preferences.get(PreferenceKey.CrashReport)
        )

    private suspend fun notifyImgCacheReplicationPref() =
        viewState.setImgCacheReplicationPref(
            preferences.get(PreferenceKey.ImgCacheReplication)
        )

    private suspend fun notifyIndexReplicationPref() =
        viewState.setIndexReplicationPref(
            preferences.get(PreferenceKey.IndexReplication)
        )

    private suspend fun notifyRemovingLostResourcesTags() =
        viewState.setRemovingLostResourcesTags(
            preferences.get(PreferenceKey.RemovingLostResourcesTags)
        )

    private suspend fun notifyBackupPref() = viewState.setBackup(
        preferences.get(PreferenceKey.BackupEnabled)
    )
}
