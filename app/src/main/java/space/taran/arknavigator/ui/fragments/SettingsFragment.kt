package space.taran.arknavigator.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import moxy.MvpAppCompatFragment
import moxy.ktx.moxyPresenter
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.FragmentSettingsBinding
import space.taran.arknavigator.mvp.presenter.SettingsPresenter
import space.taran.arknavigator.mvp.view.SettingsView
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.ui.activity.MainActivity
import space.taran.arknavigator.ui.fragments.dialog.ConfirmationDialogFragment
import space.taran.arknavigator.ui.fragments.dialog.InfoDialogFragment
import space.taran.arknavigator.ui.fragments.utils.Notifications
import space.taran.arknavigator.utils.SETTINGS_SCREEN

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
        presenter.onCreateView()
        toggleInactiveFeatures()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(SETTINGS_SCREEN, "view created in ResourcesFragment")
        super.onViewCreated(view, savedInstanceState)

        App.instance.appComponent.inject(this)

        binding.apply {
            crashReportSwitch.setOnUserCheckedChangeListener { v, b ->
                presenter.onCrashReportingClick(
                    b,
                    v?.isPressed ?: false
                )
            }

            cacheReplicationSwitch.setOnUserCheckedChangeListener { v, b ->
                presenter.onImgCacheReplicationClick(
                    b,
                    v?.isPressed ?: false
                )
            }

            indexReplicationSwitch.setOnUserCheckedChangeListener { v, b ->
                presenter.onIndexReplicationClick(
                    b,
                    v?.isPressed ?: false
                )
            }

            crashInfo.setOnClickListener {
                showInfoDialog(
                    R.string.what_are_crash_reports_,
                    R.string.crash_reports_explanation
                )
            }

            imgCacheInfo.setOnClickListener {
                showInfoDialog(
                    R.string.what_is_image_replication_,
                    R.string.explanation_of_this_feature
                )
            }

            indexReplicationInfo.setOnClickListener {
                showInfoDialog(
                    R.string.what_is_index_replication_,
                    R.string.explanation_of_this_feature
                )
            }

            resetPreferences.setOnClickListener {
                val dialog = ConfirmationDialogFragment.newInstance(
                    getString(R.string.are_you_sure_),
                    getString(R.string.all_preferences_will_be_reset_to_default),
                    getString(R.string.yes),
                    getString(R.string.no)
                )
                dialog.show(
                    childFragmentManager,
                    ConfirmationDialogFragment.CONFIRMATION_DIALOG_TAG
                )
            }
        }
    }

    override fun init() {
        Log.d(SETTINGS_SCREEN, "initializing SettingsFragment")
        (activity as MainActivity).setSelectedTab(R.id.page_settings)
        (activity as MainActivity).setToolbarVisibility(false)
        (requireActivity() as MainActivity).setBottomNavigationVisibility(true)

        childFragmentManager.setFragmentResultListener(
            ConfirmationDialogFragment.POSITIVE_KEY,
            this
        ) { _, _ ->
            presenter.onResetPreferencesClick()
        }
    }

    private fun showInfoDialog(
        @StringRes titleRes: Int,
        @StringRes descriptionRes: Int
    ) {
        val title = getString(titleRes)
        val description = getString(descriptionRes)

        val dialog = InfoDialogFragment.newInstance(title, description)
        dialog.show(childFragmentManager, InfoDialogFragment.BASE_INFO_DIALOG_TAG)
    }

    private fun toggleInactiveFeatures(isEnabled: Boolean = false) {
        binding.apply {
            crashReportSwitch.isEnabled = isEnabled
            cacheReplicationSwitch.isEnabled = isEnabled
            indexReplicationSwitch.isEnabled = isEnabled
        }
    }

    override fun setCrashReportPreference(isCrashReportEnabled: Boolean) {
        binding.crashReportSwitch.apply {
            if (isChecked != isCrashReportEnabled)
                toggleSwitchSilent(isCrashReportEnabled)
        }
    }

    override fun setImgCacheReplicationPref(isImgReplicationEnabled: Boolean) {
        binding.cacheReplicationSwitch.apply {
            if (isChecked != isImgReplicationEnabled)
                toggleSwitchSilent(isImgReplicationEnabled)
        }
    }

    override fun setIndexReplicationPref(isIndexReplication: Boolean) {
        binding.indexReplicationSwitch.apply {
            if (isChecked != isIndexReplication)
                toggleSwitchSilent(isIndexReplication)
        }
    }

    override fun notifyUser(message: String, moreTime: Boolean) {
        Notifications.notifyUser(context, message, moreTime)
    }

    override fun notifyUser(messageID: Int, moreTime: Boolean) {
        Notifications.notifyUser(context, messageID, moreTime)
    }

    companion object {
        fun newInstance() =
            SettingsFragment().apply {
                Log.d(SETTINGS_SCREEN, "creating new instance")
            }
    }
}
