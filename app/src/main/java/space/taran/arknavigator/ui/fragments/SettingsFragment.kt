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
            crashReportSwitch.setOnCheckedChangeListener { compoundButton, b ->
                presenter.onCrashReportingClick(b)
            }

            cacheReplicationSwitch.setOnCheckedChangeListener { compoundButton, b ->
                presenter.onImgCacheReplicationClick(b)
            }

            indexReplicationSwitch.setOnCheckedChangeListener { compoundButton, b ->
                presenter.onIndexReplicationClick(b)
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
        (activity as MainActivity).setSelectedTab(0)
        (activity as MainActivity).setToolbarVisibility(false)
        (requireActivity() as MainActivity).setBottomNavigationVisibility(true)
    }

    override fun setCrashReportPreference(isCrashReportEnabled: Boolean) {
        binding.crashReportSwitch.apply {
            if (isChecked != isCrashReportEnabled) isChecked = isCrashReportEnabled
        }
    }

    override fun setImgCacheReplicationPref(isImgReplicationEnabled: Boolean) {
        binding.cacheReplicationSwitch.apply {
            if (isChecked != isImgReplicationEnabled) isChecked = isImgReplicationEnabled
        }
    }

    override fun setIndexReplicationPref(isIndexReplication: Boolean) {
        binding.indexReplicationSwitch.apply {
            if (isChecked != isIndexReplication) isChecked = isIndexReplication
        }
    }

    override fun notifyUser(message: String, moreTime: Boolean) {
        Notifications.notifyUser(context, message, moreTime)
    }

    companion object {
        fun newInstance() =
            SettingsFragment().apply {
                Log.d(SETTINGS_SCREEN, "creating new instance")
            }
    }
}