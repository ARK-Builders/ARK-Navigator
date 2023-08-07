package dev.arkbuilders.navigator.presentation.screen.settings

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import dev.arkbuilders.navigator.R
import dev.arkbuilders.navigator.databinding.FragmentSettingsBinding
import dev.arkbuilders.navigator.databinding.ItemBooleanPreferenceBinding
import dev.arkbuilders.navigator.data.preferences.PreferenceKey
import dev.arkbuilders.navigator.data.preferences.Preferences
import dev.arkbuilders.navigator.presentation.App
import dev.arkbuilders.navigator.presentation.screen.main.MainActivity
import dev.arkbuilders.navigator.presentation.navigation.AppRouter
import dev.arkbuilders.navigator.presentation.navigation.Screens
import dev.arkbuilders.navigator.presentation.dialog.ConfirmationDialogFragment
import dev.arkbuilders.navigator.presentation.dialog.InfoDialogFragment
import dev.arkbuilders.navigator.presentation.utils.toast
import dev.arkbuilders.navigator.data.utils.LogTags.SETTINGS_SCREEN
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val binding by viewBinding(FragmentSettingsBinding::bind)
    private val adapter = ItemAdapter<BooleanPreferenceItem>()

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var router: AppRouter

    //region booleanPreferenceModels

    private val booleanPreferenceModels = listOf(
        BooleanPreferenceModel(
            PreferenceKey.CrashReport,
            R.string.crash_reports,
            R.string.what_are_crash_reports_,
            R.string.crash_reports_explanation,
            R.string.crash_reporting_enabled,
            R.string.crash_reporting_disabled
        ),
        BooleanPreferenceModel(
            PreferenceKey.ImgCacheReplication,
            R.string.images_cache_replication,
            R.string.what_is_image_replication_,
            R.string.explanation_of_this_feature,
            R.string.images_cache_replication_enabled,
            R.string.images_cache_replication_disabled
        ),
        BooleanPreferenceModel(
            PreferenceKey.IndexReplication,
            R.string.index_replication,
            R.string.what_is_index_replication_,
            R.string.explanation_of_this_feature,
            R.string.index_replication_enabled,
            R.string.index_replication_disabled
        ),
        BooleanPreferenceModel(
            PreferenceKey.RemovingLostResourcesTags,
            R.string.removing_lost_resources_tags,
            R.string.what_is_removing_tags,
            R.string.explanation_of_this_feature,
            R.string.removing_tags_enabled,
            R.string.removing_tags_disabled
        ),
        BooleanPreferenceModel(
            PreferenceKey.BackupEnabled,
            R.string.backup,
            R.string.what_is_backup,
            R.string.explanation_of_this_feature,
            R.string.backup_enabled,
            R.string.backup_disabled
        ),
        BooleanPreferenceModel(
            PreferenceKey.ShortFileNames,
            R.string.short_file_names,
            R.string.what_is_backup,
            R.string.explanation_of_this_feature,
            R.string.short_names_enabled,
            R.string.short_names_disabled
        ),
        BooleanPreferenceModel(
            PreferenceKey.CollectTagUsageStats,
            R.string.collect_tag_usage_stats,
            R.string.collect_tag_usage_stats,
            R.string.explanation_of_this_feature,
            R.string.collect_tag_usage_stats_enabled,
            R.string.collect_tag_usage_stats_disabled
        ),
    )

    //endregion

    override fun onAttach(context: Context) {
        App.instance.appComponent.inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).setSelectedTab(R.id.page_settings)
        (requireActivity() as MainActivity).setBottomNavigationVisibility(true)

        binding.rvPreference.layoutManager =
            object : LinearLayoutManager(requireContext()) {
                override fun canScrollVertically() = false
            }
        binding.rvPreference.adapter = FastAdapter.with(adapter)
        adapter.set(
            booleanPreferenceModels.map { model ->
                BooleanPreferenceItem(
                    model,
                    requireContext(),
                    childFragmentManager,
                    lifecycleScope,
                    preferences
                )
            }
        )

        binding.resetPreferences.setOnClickListener {
            val dialog = ConfirmationDialogFragment.newInstance(
                getString(R.string.are_you_sure),
                getString(R.string.all_preferences_will_be_reset_to_default),
                getString(R.string.yes),
                getString(R.string.no)
            )
            dialog.show(
                childFragmentManager,
                ConfirmationDialogFragment.CONFIRMATION_DIALOG_TAG
            )
        }

        binding.btnRescan.setOnClickListener {
            router.newRootScreen(Screens.FoldersScreenRescanRoots())
        }

        childFragmentManager.setFragmentResultListener(
            ConfirmationDialogFragment.DEFAULT_POSITIVE_REQUEST_KEY,
            this
        ) { _, _ ->
            lifecycleScope.launch {
                preferences.clearPreferences()
                adapter.fastAdapter?.notifyDataSetChanged()
            }
        }
    }

    companion object {
        fun newInstance() =
            SettingsFragment().apply {
                Log.d(SETTINGS_SCREEN, "creating new instance")
            }
    }
}

private class BooleanPreferenceModel(
    val key: PreferenceKey<Boolean>,
    @StringRes val name: Int,
    @StringRes val title: Int,
    @StringRes val desc: Int,
    @StringRes val toastEnabled: Int,
    @StringRes val toastDisabled: Int,
)

private class BooleanPreferenceItem(
    val model: BooleanPreferenceModel,
    val ctx: Context,
    val fragmentManager: FragmentManager,
    val lifecycleScope: CoroutineScope,
    val preferences: Preferences
) : AbstractBindingItem<ItemBooleanPreferenceBinding>() {
    override val type = R.id.fastadapter_item
    private var preferenceEnabled = false

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ) = ItemBooleanPreferenceBinding.inflate(inflater, parent, false)

    override fun bindView(
        binding: ItemBooleanPreferenceBinding,
        payloads: List<Any>
    ) = with(binding) {
        tvName.text = ctx.getString(model.name)
        btnInfo.setOnClickListener {
            val title = ctx.getString(model.title)
            val description = ctx.getString(model.desc)

            val dialog = InfoDialogFragment.newInstance(title, description)
            dialog.show(fragmentManager, InfoDialogFragment.BASE_INFO_DIALOG_TAG)
        }
        lifecycleScope.launch {
            preferenceEnabled = preferences.get(model.key)
            switchBtn.isChecked = preferenceEnabled
            setupSwitchListener(switchBtn)
        }
        return@with
    }

    private fun setupSwitchListener(switchBtn: View) {
        switchBtn.setOnClickListener {
            preferenceEnabled = !preferenceEnabled
            ctx.toast(
                if (preferenceEnabled) model.toastEnabled
                else model.toastDisabled
            )
            lifecycleScope.launch {
                preferences.set(model.key, preferenceEnabled)
            }
        }
    }
}
