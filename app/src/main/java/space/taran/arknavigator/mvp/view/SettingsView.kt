package space.taran.arknavigator.mvp.view

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType

@StateStrategyType(AddToEndSingleStrategy::class)
interface SettingsView : MvpView {
    fun init()
    fun setCrashReportPreference(isCrashReportEnabled: Boolean)
    fun setImgCacheReplicationPref(isImgReplicationEnabled: Boolean)
    fun setIndexReplicationPref(isIndexReplication: Boolean)
    fun setRemovingLostResourcesTags(enabled: Boolean)

    @StateStrategyType(SkipStrategy::class)
    fun toastCrashReportingEnabled(enabled: Boolean)
    @StateStrategyType(SkipStrategy::class)
    fun toastImageCacheReplicationEnabled(enabled: Boolean)
    @StateStrategyType(SkipStrategy::class)
    fun toastIndexReplicationEnabled(enabled: Boolean)
    @StateStrategyType(SkipStrategy::class)
    fun toastRemovingTagsEnabled(enabled: Boolean)
}
