package space.taran.arknavigator.mvp.presenter

import android.util.Log
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import ru.terrakok.cicerone.Router
import space.taran.arknavigator.mvp.model.UserPreferences
import space.taran.arknavigator.mvp.view.SettingsView
import space.taran.arknavigator.utils.SETTINGS_SCREEN
import javax.inject.Inject

class SettingsPresenter : MvpPresenter<SettingsView>() {

    @Inject
    lateinit var router: Router

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onFirstViewAttach() {
        Log.d(SETTINGS_SCREEN, "first view attached in SettingsPresenter")
        super.onFirstViewAttach()

        viewState.init()
        presenterScope.launch {
            notifyAllPreferences()
        }
    }

    fun onCrashReportingClick(isCrashReportEnabled: Boolean) {
        presenterScope.launch {
            Log.d(SETTINGS_SCREEN, "Saving crash report preference, is enabled: $isCrashReportEnabled")
            userPreferences.setCrashReportEnabled(isCrashReportEnabled)
        }
    }

    fun onImgCacheReplicationClick(cacheReplicationEnabled: Boolean) {
        presenterScope.launch {
            Log.d(SETTINGS_SCREEN, "Saving imgCacheReplication preference, is enabled: $cacheReplicationEnabled")
            userPreferences.setCacheReplicationEnabled(cacheReplicationEnabled)
        }
    }

    fun onIndexReplicationClick(indexReplicationEnabled: Boolean) {
        presenterScope.launch {
            Log.d(SETTINGS_SCREEN, "Saving indexReplication preference, is enabled: $indexReplicationEnabled")
            userPreferences.setIndexReplicationEnabled(indexReplicationEnabled)
        }
    }

    fun onResetPreferencesClick() {
        presenterScope.launch {
            userPreferences.clearPreferences()
            notifyAllPreferences()
        }
    }

    private suspend fun notifyAllPreferences(){
        notifyCrashReportPref()
        notifyImgCacheReplicationPref()
        notifyIndexReplicationPref()
    }

    private suspend fun notifyCrashReportPref() =
        viewState.setCrashReportPreference(userPreferences.isCrashReportEnabled())

    private suspend fun notifyImgCacheReplicationPref() =
        viewState.setImgCacheReplicationPref(userPreferences.isCacheReplicationEnabled())

    private suspend fun notifyIndexReplicationPref() =
        viewState.setIndexReplicationPref(userPreferences.isIndexReplicationEnabled())
}