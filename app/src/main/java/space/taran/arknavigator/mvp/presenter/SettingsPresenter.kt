package space.taran.arknavigator.mvp.presenter

import android.util.Log
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import space.taran.arknavigator.R
import space.taran.arknavigator.mvp.model.UserPreferences
import space.taran.arknavigator.mvp.view.SettingsView
import space.taran.arknavigator.navigation.AppRouter
import space.taran.arknavigator.utils.LogTags.SETTINGS_SCREEN
import javax.inject.Inject

class SettingsPresenter : MvpPresenter<SettingsView>() {

    @Inject
    lateinit var router: AppRouter

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

    fun onCrashReportingClick(
        enabled: Boolean
    ) {
        presenterScope.launch {
            viewState.notifyUser(
                if (enabled)
                    R.string.crash_reporting_enabled else
                    R.string.crash_reporting_disabled
            )
            Log.d(
                SETTINGS_SCREEN,
                "Saving crash report preference, is enabled: $enabled"
            )
            userPreferences.setCrashReportEnabled(enabled)
            notifyCrashReportPref()
        }
    }

    fun onImgCacheReplicationClick(enabled: Boolean) {
        presenterScope.launch {
            viewState.notifyUser(
                if (enabled)
                    R.string.images_cache_replication_enabled else
                    R.string.images_cache_replication_disabled
            )
            Log.d(
                SETTINGS_SCREEN,
                "Saving imgCacheReplication preference, " +
                    "is enabled: $enabled"
            )
            userPreferences.setCacheReplicationEnabled(enabled)
            notifyImgCacheReplicationPref()
        }
    }

    fun onIndexReplicationClick(enabled: Boolean) {
        presenterScope.launch {
            viewState.notifyUser(
                if (enabled)
                    R.string.index_replication_enabled else
                    R.string.index_replication_disabled
            )
            Log.d(
                SETTINGS_SCREEN,
                "Saving indexReplication preference, " +
                    "is enabled: $enabled"
            )
            userPreferences.setIndexReplicationEnabled(enabled)
            notifyIndexReplicationPref()
        }
    }

    fun onRemovingLostResourcesTagsClick(checked: Boolean) {
        presenterScope.launch {
            viewState.notifyUser(
                if (checked)
                    R.string.removing_tags_enabled else
                    R.string.removing_tags_disabled
            )
            userPreferences.setRemovingLostResourcesTagsEnabled(checked)
            notifyRemovingLostResourcesTags()
        }
    }

    fun onResetPreferencesClick() {
        presenterScope.launch {
            userPreferences.clearPreferences()
            notifyAllPreferences()
        }
    }

    private suspend fun notifyAllPreferences() {
        notifyCrashReportPref()
        notifyImgCacheReplicationPref()
        notifyIndexReplicationPref()
        notifyRemovingLostResourcesTags()
    }

    private suspend fun notifyCrashReportPref() =
        viewState.setCrashReportPreference(
            userPreferences.isCrashReportEnabled()
        )

    private suspend fun notifyImgCacheReplicationPref() =
        viewState.setImgCacheReplicationPref(
            userPreferences.isCacheReplicationEnabled()
        )

    private suspend fun notifyIndexReplicationPref() =
        viewState.setIndexReplicationPref(
            userPreferences.isIndexReplicationEnabled()
        )

    private suspend fun notifyRemovingLostResourcesTags() =
        viewState.setRemovingLostResourcesTags(
            userPreferences.isRemovingLostResourcesTagsEnabled()
        )
}
