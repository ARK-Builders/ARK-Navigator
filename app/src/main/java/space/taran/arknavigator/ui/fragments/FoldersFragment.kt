package space.taran.arknavigator.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import moxy.MvpAppCompatFragment
import moxy.ktx.moxyPresenter
import space.taran.arkfilepicker.folders.FoldersRepo.Companion.DELETE_FOLDER_KEY
import space.taran.arkfilepicker.folders.FoldersRepo.Companion.FAVORITE_KEY
import space.taran.arkfilepicker.folders.FoldersRepo.Companion.FORGET_FAVORITE_KEY
import space.taran.arkfilepicker.folders.FoldersRepo.Companion.FORGET_ROOT_KEY
import space.taran.arkfilepicker.folders.FoldersRepo.Companion.ROOT_KEY
import space.taran.arkfilepicker.presentation.folderstree.FolderNode
import space.taran.arkfilepicker.presentation.folderstree.DeviceNode
import space.taran.arkfilepicker.presentation.folderstree.RootNode
import space.taran.arkfilepicker.presentation.folderstree.FavoriteNode
import space.taran.arkfilepicker.presentation.folderstree.FolderTreeView
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.FragmentFoldersBinding
import space.taran.arknavigator.mvp.presenter.FoldersPresenter
import space.taran.arknavigator.mvp.view.FoldersView
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.ui.activity.MainActivity
import space.taran.arknavigator.ui.fragments.dialog.ConfirmationDialogFragment
import space.taran.arknavigator.ui.fragments.dialog.RootPickerDialogFragment
import space.taran.arknavigator.ui.fragments.dialog.RootsScanDialogFragment
import space.taran.arknavigator.ui.fragments.dialog.onRootOrFavPicked
import space.taran.arknavigator.ui.fragments.utils.toast
import space.taran.arknavigator.ui.fragments.utils.toastFailedPaths
import space.taran.arknavigator.ui.view.StackedToasts
import space.taran.arknavigator.utils.FullscreenHelper
import space.taran.arknavigator.utils.LogTags.FOLDERS_SCREEN
import java.nio.file.Path
import kotlin.io.path.Path

class FoldersFragment : MvpAppCompatFragment(), FoldersView {

    private lateinit var binding: FragmentFoldersBinding
    private val presenter by moxyPresenter {
        FoldersPresenter(
            requireArguments().getBoolean(RESCAN_ROOTS_BUNDLE_KEY, false)
        ).apply {
            Log.d(FOLDERS_SCREEN, "RootsPresenter created")
            App.instance.appComponent.inject(this)
        }
    }
    private lateinit var stackedToasts: StackedToasts
    private var foldersTree: FolderTreeView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(FOLDERS_SCREEN, "inflating layout for FoldersFragment")
        binding = FragmentFoldersBinding.inflate(inflater, container, false)
        FullscreenHelper.setStatusBarVisibility(true, requireActivity().window)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(FOLDERS_SCREEN, "view created in FoldersFragment")
        super.onViewCreated(view, savedInstanceState)
        App.instance.appComponent.inject(this)
    }

    override fun init() {
        Log.d(FOLDERS_SCREEN, "initializing FoldersFragment")
        (activity as MainActivity).setSelectedTab(R.id.page_roots)
        stackedToasts = StackedToasts(binding.rvToasts, lifecycleScope)
        binding.rvRoots.layoutManager = LinearLayoutManager(context)
        foldersTree = FolderTreeView(
            binding.rvRoots,
            onNavigateClick = presenter::onNavigateBtnClick,
            onAddClick = { presenter.onFoldersTreeAddFavoriteBtnClick(it) },
            onForgetClick = presenter::onForgetBtnClick,
            showOptions = true
        )

        initResultListeners()

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            presenter.onBackClick()
        }

        binding.fabAddRoots.setOnClickListener {
            presenter.onAddRootBtnClick()
        }
    }

    override fun setProgressVisibility(isVisible: Boolean, withText: String) {
        binding.layoutProgress.apply {
            root.isVisible = isVisible
            (activity as MainActivity).setBottomNavigationEnabled(!isVisible)

            if (withText.isNotEmpty()) {
                progressText.setVisibilityAndLoadingStatus(View.VISIBLE)
                progressText.loadingText = withText
            } else progressText.setVisibilityAndLoadingStatus(View.GONE)
        }
    }

    override fun updateFoldersTree(
        devices: List<Path>,
        rootsWithFavs: Map<Path, List<Path>>
    ) {
        if(rootsWithFavs.size < 1){
            binding.noFolderHint.setVisibility(View.VISIBLE) }
        else binding.noFolderHint.setVisibility(View.INVISIBLE)
        foldersTree?.set(devices, rootsWithFavs)
    }

    override fun openRootPickerDialog(path: Path?) =
        RootPickerDialogFragment.newInstance(path)
            .show(childFragmentManager, null)

    override fun openConfirmForgetFolderDialog(node: FolderNode) {
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

    override fun toastFailedPath(failedPaths: List<Path>) =
        toastFailedPaths(failedPaths)

    override fun openRootsScanDialog() =
        RootsScanDialogFragment.newInstance().show(childFragmentManager, null)

    override fun toastRootIsAlreadyPicked() =
        toast(R.string.folders_root_is_already_picked)

    override fun toastFavoriteIsAlreadyPicked() =
        toast(R.string.folders_favorite_is_already_picked)

    override fun toastIndexingCanTakeMinutes() =
        toast(R.string.toast_indexing_can_take_minutes)

    override fun toastIndexFailedPath(path: Path) {
        stackedToasts.toast(path)
    }

    private fun initResultListeners() {
        childFragmentManager
            .onRootOrFavPicked(this) { path, rootNotFavorite ->
                presenter.onPickRootBtnClick(path, rootNotFavorite)
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

            presenter.onRootsFound(roots)
        }

        childFragmentManager.setFragmentResultListener(
            FORGET_ROOT_KEY,
            this
        ) { _, bundle ->
            presenter.onForgetRoot(
                Path(bundle.getString(ROOT_KEY, "")),
                bundle.getBoolean(DELETE_FOLDER_KEY)
            )
        }

        childFragmentManager.setFragmentResultListener(
            FORGET_FAVORITE_KEY,
            this
        ) { _, bundle ->
            presenter.onForgetFavorite(
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
