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
import space.taran.arknavigator.ui.fragments.utils.toast
import space.taran.arknavigator.utils.LogTags.SETTINGS_SCREEN

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

    override fun init() {
        Log.d(SETTINGS_SCREEN, "initializing SettingsFragment")
        (activity as MainActivity).setSelectedTab(R.id.page_settings)
        (activity as MainActivity).setToolbarVisibility(false)
        (requireActivity() as MainActivity).setBottomNavigationVisibility(true)
        initListeners()

        childFragmentManager.setFragmentResultListener(
            ConfirmationDialogFragment.POSITIVE_KEY,
            this
        ) { _, _ ->
            presenter.onResetPreferencesClick()
        }
    }

    private fun initListeners() = binding.apply {
        crashReportSwitch.setOnUserCheckedChangeListener {
            presenter.onCrashReportingClick(it)
        }

        cacheReplicationSwitch.setOnUserCheckedChangeListener {
            presenter.onImgCacheReplicationClick(it)
        }

        indexReplicationSwitch.setOnUserCheckedChangeListener {
            presenter.onIndexReplicationClick(it)
        }

        switchRemovingTags.setOnUserCheckedChangeListener {
            presenter.onRemovingLostResourcesTagsClick(it)
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

        infoRemovingTags.setOnClickListener {
            showInfoDialog(
                R.string.what_is_removing_tags,
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

    private fun showInfoDialog(
        @StringRes titleRes: Int,
        @StringRes descriptionRes: Int
    ) {
        val title = getString(titleRes)
        val description = getString(descriptionRes)

        val dialog = InfoDialogFragment.newInstance(title, description)
        dialog.show(childFragmentManager, InfoDialogFragment.BASE_INFO_DIALOG_TAG)
    }

    override fun setCrashReportPreference(isCrashReportEnabled: Boolean) =
        binding.crashReportSwitch.toggleSwitchSilent(isCrashReportEnabled)

    override fun setImgCacheReplicationPref(isImgReplicationEnabled: Boolean) =
        binding.cacheReplicationSwitch.toggleSwitchSilent(isImgReplicationEnabled)

    override fun setIndexReplicationPref(isIndexReplication: Boolean) =
        binding.indexReplicationSwitch.toggleSwitchSilent(isIndexReplication)

    override fun setRemovingLostResourcesTags(enabled: Boolean) =
        binding.switchRemovingTags.toggleSwitchSilent(enabled)

    override fun toastCrashReportingEnabled(enabled: Boolean) =
        toast(
            if (enabled) R.string.crash_reporting_enabled
            else R.string.crash_reporting_disabled
        )

    override fun toastImageCacheReplicationEnabled(enabled: Boolean) =
        toast(
            if (enabled) R.string.images_cache_replication_enabled
            else R.string.images_cache_replication_disabled
        )

    override fun toastIndexReplicationEnabled(enabled: Boolean) =
        toast(
            if (enabled) R.string.index_replication_enabled
            else R.string.index_replication_disabled
        )

    override fun toastRemovingTagsEnabled(enabled: Boolean) =
        toast(
            if (enabled) R.string.removing_tags_enabled
            else R.string.removing_tags_disabled
        )

    companion object {
        fun newInstance() =
            SettingsFragment().apply {
                Log.d(SETTINGS_SCREEN, "creating new instance")
            }
    }
}
