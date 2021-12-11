package space.taran.arknavigator.mvp.presenter

import android.util.Log
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import ru.terrakok.cicerone.Router
import space.taran.arknavigator.mvp.model.UserPreferences
import space.taran.arknavigator.mvp.model.UserPreferences.*
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

    fun onCrashReportingClick(crashReport: CrashReport) {
        presenterScope.launch {
            Log.d(SETTINGS_SCREEN, "Saving crash report preference: $crashReport")
            userPreferences.setCrashReportPref(crashReport)
        }
    }

    fun onImgCacheReplicationClick(imgCacheReplication: ImgCacheReplication) {
        presenterScope.launch {
            Log.d(SETTINGS_SCREEN, "Saving imgCacheReplication preference: $imgCacheReplication")
            userPreferences.setImgCacheReplication(imgCacheReplication)
        }
    }

    fun onIndexReplicationClick(indexReplication: IndexReplication) {
        presenterScope.launch {
            Log.d(SETTINGS_SCREEN, "Saving indexReplication preference: $indexReplication")
            userPreferences.setIndexReplication(indexReplication)
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
        viewState.setCrashReportPreference(userPreferences.getCrashReportPref())

    private suspend fun notifyImgCacheReplicationPref() =
        viewState.setImgCacheReplicationPref(userPreferences.getImgCacheReplication())

    private suspend fun notifyIndexReplicationPref() =
        viewState.setIndexReplicationPref(userPreferences.getIndexReplication())
}