package space.taran.arknavigator.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.children
import moxy.MvpAppCompatFragment
import moxy.ktx.moxyPresenter
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.FragmentSettingsBinding
import space.taran.arknavigator.mvp.model.UserPreferences.*
import space.taran.arknavigator.mvp.presenter.SettingsPresenter
import space.taran.arknavigator.mvp.view.SettingsView
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.ui.activity.MainActivity
import space.taran.arknavigator.ui.fragments.utils.Notifications
import space.taran.arknavigator.utils.SETTINGS_SCREEN
import space.taran.arknavigator.utils.showInfoDialog
import java.lang.AssertionError

class SettingsFragment : MvpAppCompatFragment(), SettingsView {

    private lateinit var binding: FragmentSettingsBinding

    private val presenter by moxyPresenter {
        SettingsPresenter().apply {
            Log.d(SETTINGS_SCREEN, "creating SettingsPresenter")
            App.instance.appComponent.inject(this)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        Log.d(SETTINGS_SCREEN, "inflating layout for SettingsFragment")
        binding = FragmentSettingsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(SETTINGS_SCREEN, "view created in ResourcesFragment")
        super.onViewCreated(view, savedInstanceState)

        App.instance.appComponent.inject(this)

        binding.apply {
            crashGroup.setOnCheckedChangeListener { rg, checkedID ->
                presenter.onCrashReportingClick(crashReportFromButton(checkedID))
                rgButtonSelected(rg, checkedID)
            }

            imgReplicationGroup.setOnCheckedChangeListener { rg, checkedID ->
                presenter.onImgCacheReplicationClick(imgReplicationFromButton(checkedID))
                rgButtonSelected(rg, checkedID)
            }

            indexReplicationGroup.setOnCheckedChangeListener { rg, checkedID ->
                presenter.onIndexReplicationClick(indexReplicationFromButton(checkedID))
                rgButtonSelected(rg, checkedID)
            }

            crashInfo.setOnClickListener {
                showInfoDialog(
                    requireContext(),
                    title = R.string.what_are_crash_reports_,
                    descrText = R.string.crash_reports_explanation
                )
            }

            imgCacheInfo.setOnClickListener {
                showInfoDialog(
                    requireContext(),
                    title = R.string.what_is_image_replication_,
                    descrText = R.string.explanation_of_this_feature
                )
            }

            indexReplicationInfo.setOnClickListener {
                showInfoDialog(
                    requireContext(),
                    title = R.string.what_is_index_replication_,
                    descrText = R.string.explanation_of_this_feature
                )
            }

            resetPreferences.setOnClickListener {
                showInfoDialog(
                    requireContext(),
                    title = R.string.are_you_sure_,
                    descrText = R.string.all_preferences_will_be_reset_to_default,
                    posButtonText = R.string.yes,
                    negButtonText = R.string.no,
                    posButtonCallback = { presenter.onResetPreferencesClick() }
                )
            }
        }
    }

    override fun init() {
        Log.d(SETTINGS_SCREEN, "initializing SettingsFragment")
        (activity as MainActivity).setSelectedTab(2)
        (activity as MainActivity).setToolbarVisibility(false)
        (requireActivity() as MainActivity).setBottomNavigationVisibility(true)
    }

    override fun setCrashReportPreference(crashReport: CrashReport) {
        binding.crashGroup.check(buttonFromCrashReport(crashReport))
    }

    override fun setImgCacheReplicationPref(imgReplicationPref: ImgCacheReplication) {
        binding.imgReplicationGroup.check(buttonFromImgReplication(imgReplicationPref))
    }

    override fun setIndexReplicationPref(indexReplication: IndexReplication) {
        binding.indexReplicationGroup.check(buttonFromIndexReplication(indexReplication))
    }

    override fun notifyUser(message: String, moreTime: Boolean) {
        Notifications.notifyUser(context, message, moreTime)
    }

    private fun rgButtonSelected(rg: RadioGroup, checkedID: Int) {
        rg.children.filterIsInstance(RadioButton::class.java).forEach { rb ->
            if (rb.id != checkedID) rb.setTextColor(getColor(R.color.colorPrimary))
            else rb.setTextColor(getColor(R.color.white))
        }
    }

    private fun getColor(@ColorRes colorID: Int) =
        ContextCompat.getColor(requireContext(), colorID)

    private fun crashReportFromButton(buttonID: Int): CrashReport {
        val reversed = crashReportToBtnMap.entries.associate { (k, v) -> v to k }
        return reversed.getOrDefault(buttonID, CrashReport.NONE)
    }

    private fun buttonFromCrashReport(crashReport: CrashReport): Int {
        return crashReportToBtnMap[crashReport]
            ?: throw AssertionError("CrashReport must be of known type")
    }

    private fun imgReplicationFromButton(buttonID: Int): ImgCacheReplication {
        val reversed = imgReplicationToBtnMap.entries.associate { (k, v) -> v to k }
        return reversed.getOrDefault(buttonID, ImgCacheReplication.ENABLED)
    }

    private fun buttonFromImgReplication(imgReplication: ImgCacheReplication): Int {
        return imgReplicationToBtnMap[imgReplication]
            ?: throw AssertionError("ImgCacheReplication must be of known type")
    }

    private fun indexReplicationFromButton(buttonID: Int): IndexReplication {
        val reversed = indexReplicationToBtnMap.entries.associate { (k, v) -> v to k }
        return reversed.getOrDefault(buttonID, IndexReplication.ENABLED)
    }

    private fun buttonFromIndexReplication(indexReplication: IndexReplication): Int {
        return indexReplicationToBtnMap[indexReplication]
            ?: throw AssertionError("IndexReplication must be of known type")
    }

    private val crashReportToBtnMap = mapOf(
        CrashReport.SEND_AUTOMATICALLY to R.id.crashSendAutomatically,
        CrashReport.DONT_SEND to R.id.crashDontSend
    )

    private val imgReplicationToBtnMap = mapOf(
        ImgCacheReplication.ENABLED to R.id.imgReplicationOn,
        ImgCacheReplication.DISABLED to R.id.imgReplicationOff
    )

    private val indexReplicationToBtnMap = mapOf(
        IndexReplication.ENABLED to R.id.indexReplicationOn,
        IndexReplication.DISABLED to R.id.indexReplicationOff
    )

    companion object {
        fun newInstance() =
            SettingsFragment().apply {
                Log.d(SETTINGS_SCREEN, "creating new instance")
            }
    }
}