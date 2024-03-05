package dev.arkbuilders.navigator.presentation.screen.folders

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import by.kirich1409.viewbindingdelegate.viewBinding
import dev.arkbuilders.arkfilepicker.folders.FoldersRepo.Companion.DELETE_FOLDER_KEY
import dev.arkbuilders.arkfilepicker.folders.FoldersRepo.Companion.FAVORITE_KEY
import dev.arkbuilders.arkfilepicker.folders.FoldersRepo.Companion.FORGET_FAVORITE_KEY
import dev.arkbuilders.arkfilepicker.folders.FoldersRepo.Companion.FORGET_ROOT_KEY
import dev.arkbuilders.arkfilepicker.folders.FoldersRepo.Companion.ROOT_KEY
import dev.arkbuilders.arkfilepicker.folders.RootAndFav
import dev.arkbuilders.arkfilepicker.presentation.folderstree.DeviceNode
import dev.arkbuilders.arkfilepicker.presentation.folderstree.FavoriteNode
import dev.arkbuilders.arkfilepicker.presentation.folderstree.FolderNode
import dev.arkbuilders.arkfilepicker.presentation.folderstree.FolderTreeView
import dev.arkbuilders.arkfilepicker.presentation.folderstree.RootNode
import dev.arkbuilders.navigator.R
import dev.arkbuilders.navigator.analytics.folders.FoldersAnalytics
import dev.arkbuilders.navigator.data.utils.LogTags.FOLDERS_SCREEN
import dev.arkbuilders.navigator.databinding.FragmentFoldersBinding
import dev.arkbuilders.navigator.presentation.App
import dev.arkbuilders.navigator.presentation.dialog.ConfirmationDialogFragment
import dev.arkbuilders.navigator.presentation.dialog.ExplainPermsDialog
import dev.arkbuilders.navigator.presentation.dialog.RootPickerDialogFragment
import dev.arkbuilders.navigator.presentation.dialog.onRootOrFavPicked
import dev.arkbuilders.navigator.presentation.dialog.rootsscan.RootsScanDialogFragment
import dev.arkbuilders.navigator.presentation.navigation.AppRouter
import dev.arkbuilders.navigator.presentation.navigation.Screens
import dev.arkbuilders.navigator.presentation.screen.main.MainActivity
import dev.arkbuilders.navigator.presentation.utils.FullscreenHelper
import dev.arkbuilders.navigator.presentation.utils.toast
import dev.arkbuilders.navigator.presentation.utils.toastFailedPaths
import dev.arkbuilders.navigator.presentation.view.StackedToasts
import org.orbitmvi.orbit.viewmodel.observe
import timber.log.Timber
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.Path

class FoldersFragment : Fragment(R.layout.fragment_folders) {

    private val binding by viewBinding(FragmentFoldersBinding::bind)

    @Inject
    lateinit var factory: FoldersViewModelFactory.Factory
    private val viewModel: FoldersViewModel by viewModels {
        factory.create(requireArguments().getBoolean(RESCAN_ROOTS_BUNDLE_KEY, false))
    }

    @Inject
    lateinit var router: AppRouter

    @Inject
    lateinit var folderAnalytics: FoldersAnalytics

    private lateinit var stackedToasts: StackedToasts
    private var foldersTree: FolderTreeView? = null

    override fun onAttach(context: Context) {
        App.instance.appComponent.inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.d(FOLDERS_SCREEN, "view created in FoldersFragment")
        super.onViewCreated(view, savedInstanceState)
        FullscreenHelper.setStatusBarVisibility(true, requireActivity().window)

        init()
        viewModel.observe(this, state = ::render, sideEffect = ::handleSideEffect)
    }

    fun init() {
        Timber.d(FOLDERS_SCREEN, "initializing FoldersFragment")
        (activity as MainActivity).setSelectedTab(R.id.page_roots)
        stackedToasts = StackedToasts(binding.rvToasts, lifecycleScope)
        binding.rvRoots.layoutManager = LinearLayoutManager(context)
        foldersTree = FolderTreeView(
            binding.rvRoots,
            onNavigateClick = { onFoldersTreeNavigateBtnClick(it) },
            onAddClick = { openRootPickerDialog(it.path) },
            onForgetClick = { openConfirmForgetFolderDialog(it) },
            showOptions = true
        )

        initResultListeners()

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            Timber.d(FOLDERS_SCREEN, "[back] clicked")
            router.exit()
        }

        binding.fabAddRoots.setOnClickListener {
            openRootPickerDialog(null)
        }

        binding.rvRoots.addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // Hide the Add FAB when scrolling down
                if (dy > 10 && binding.fabAddRoots.isShown) {
                    binding.fabAddRoots.hide()
                }

                // Show the Add FAB when scrolling up
                if (dy < -10 && !binding.fabAddRoots.isShown) {
                    binding.fabAddRoots.show()
                }

                // Always show the Add FAB when being on top of the list
                if (!recyclerView.canScrollVertically(-1)) {
                    binding.fabAddRoots.show()
                }
            }
        })
    }

    private fun render(state: FoldersState) = with(state) {
        binding.noFolderHint.isVisible = initialized && folders.isEmpty()
        foldersTree?.set(devices, folders)
        setProgressVisibility(
            progressWithText.enabled,
            progressWithText.text
        )
    }

    private fun handleSideEffect(effect: FoldersSideEffect) = when (effect) {
        FoldersSideEffect.OpenRootsScanDialog ->
            RootsScanDialogFragment.newInstance().show(childFragmentManager, null)

        is FoldersSideEffect.ToastFailedPaths ->
            toastFailedPaths(effect.failedPaths)

        FoldersSideEffect.ToastRootIsAlreadyPicked ->
            toast(R.string.folders_root_is_already_picked)

        FoldersSideEffect.ToastFavoriteIsAlreadyPicked ->
            toast(R.string.folders_favorite_is_already_picked)

        FoldersSideEffect.ToastIndexingCanTakeMinutes ->
            toast(R.string.toast_indexing_can_take_minutes)

        FoldersSideEffect.ShowExplainPermsDialog ->
            ExplainPermsDialog.newInstance(requireContext())
                .show(childFragmentManager, null)
    }

    fun setProgressVisibility(isVisible: Boolean, withText: String) {
        binding.layoutProgress.apply {
            root.isVisible = isVisible
            (activity as MainActivity).setBottomNavigationEnabled(!isVisible)

            if (withText.isNotEmpty()) {
                progressText.setVisibilityAndLoadingStatus(View.VISIBLE)
                progressText.loadingText = withText
            } else progressText.setVisibilityAndLoadingStatus(View.GONE)
        }
    }

    private fun openRootPickerDialog(path: Path?) =
        RootPickerDialogFragment.newInstance(path)
            .show(childFragmentManager, null)

    private fun openConfirmForgetFolderDialog(node: FolderNode) {
        val bundle = bundleOf()
        var positiveRequestKey = FORGET_ROOT_KEY
        when (node) {
            is DeviceNode -> {}
            is RootNode -> {
                bundle.putString(ROOT_KEY, node.path.toString())
            }

            is FavoriteNode -> {
                bundle.putString(ROOT_KEY, node.root.toString())
                bundle.putString(FAVORITE_KEY, node.path.toString())
                positiveRequestKey = FORGET_FAVORITE_KEY
            }
        }
        ConfirmationDialogFragment.newInstance(
            getString(R.string.are_you_sure),
            getString(R.string.folder_forget_warn),
            getString(R.string.yes),
            getString(R.string.no),
            positiveRequestKey,
            bundle
        ).show(childFragmentManager, null)
    }

    private fun onFoldersTreeNavigateBtnClick(node: FolderNode) {
        when (node) {
            is DeviceNode -> {}
            is RootNode -> {
                router.navigateTo(
                    Screens.ResourcesScreen(
                        RootAndFav(node.path.toString(), null)
                    )
                )
                folderAnalytics.trackRootOpen()
            }

            is FavoriteNode -> {
                router.navigateTo(
                    Screens.ResourcesScreen(
                        RootAndFav(node.root.toString(), node.path.toString())
                    )
                )
                folderAnalytics.trackFavOpen()
            }
        }
    }

    private fun initResultListeners() {
        childFragmentManager
            .onRootOrFavPicked(this) { path, rootNotFavorite ->
                viewModel.onPickRootBtnClick(path, rootNotFavorite)
            }

        childFragmentManager.setFragmentResultListener(
            RootsScanDialogFragment.REQUEST_KEY_ROOTS_FOUND,
            this
        ) { _, bundle ->
            val roots =
                bundle.getStringArray(RootsScanDialogFragment.RESULT_KEY_ROOTS)
                    ?.map {
                        Path(it)
                    } ?: return@setFragmentResultListener

            viewModel.onRootsFound(roots)
        }

        childFragmentManager.setFragmentResultListener(
            FORGET_ROOT_KEY,
            this
        ) { _, bundle ->
            viewModel.onForgetRoot(
                Path(bundle.getString(ROOT_KEY, "")),
                bundle.getBoolean(DELETE_FOLDER_KEY)
            )
        }

        childFragmentManager.setFragmentResultListener(
            FORGET_FAVORITE_KEY,
            this
        ) { _, bundle ->
            viewModel.onForgetFavorite(
                Path(bundle.getString(ROOT_KEY, "")),
                Path(bundle.getString(FAVORITE_KEY, "")),
                bundle.getBoolean(DELETE_FOLDER_KEY)
            )
        }
    }

    companion object {
        private const val RESCAN_ROOTS_BUNDLE_KEY = "rescanRoots"

        fun newInstance() = FoldersFragment().apply {
            arguments = bundleOf()
        }

        fun newInstance(rescan: Boolean) = FoldersFragment().apply {
            arguments = bundleOf().apply {
                putBoolean(RESCAN_ROOTS_BUNDLE_KEY, rescan)
            }
        }
    }
}
