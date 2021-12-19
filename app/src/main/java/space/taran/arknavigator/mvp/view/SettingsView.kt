package space.taran.arknavigator.mvp.view

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.StateStrategyType

@StateStrategyType(AddToEndSingleStrategy::class)
interface SettingsView: MvpView, NotifiableView {
    fun init()
    fun setCrashReportPreference(isCrashReportEnabled: Boolean)
    fun setImgCacheReplicationPref(isImgReplicationEnabled: Boolean)
    fun setIndexReplicationPref(isIndexReplication: Boolean)
}