package space.taran.arknavigator.mvp.view

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.StateStrategyType
import space.taran.arknavigator.mvp.model.UserPreferences.*

@StateStrategyType(AddToEndSingleStrategy::class)
interface SettingsView: MvpView, NotifiableView {
    fun init()
    fun setCrashReportPreference(crashReport: CrashReport)
    fun setImgCacheReplicationPref(imgReplicationPref: ImgCacheReplication)
    fun setIndexReplicationPref(indexReplication: IndexReplication)
}