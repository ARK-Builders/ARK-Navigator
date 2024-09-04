package dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.viewpager2.widget.ViewPager2
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.chip.Chip
import dev.arkbuilders.arkfilepicker.folders.RootAndFav
import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.data.index.Resource
import dev.arkbuilders.arklib.data.index.ResourceIndex
import dev.arkbuilders.arklib.data.meta.Metadata
import dev.arkbuilders.arklib.user.tags.Tag
import dev.arkbuilders.arklib.user.tags.TagStorage
import dev.arkbuilders.arklib.user.tags.Tags
import dev.arkbuilders.arklib.utils.extension
import dev.arkbuilders.components.databinding.ScoreWidgetBinding
import dev.arkbuilders.components.scorewidget.ScoreWidget
import dev.arkbuilders.navigator.BuildConfig
import dev.arkbuilders.navigator.R
import dev.arkbuilders.navigator.data.stats.StatsStorage
import dev.arkbuilders.navigator.data.utils.LogTags
import dev.arkbuilders.navigator.databinding.FragmentGalleryBinding
import dev.arkbuilders.navigator.databinding.PopupGalleryTagMenuBinding
import dev.arkbuilders.navigator.presentation.App
import dev.arkbuilders.navigator.presentation.dialog.DetailsAlertDialog
import dev.arkbuilders.navigator.presentation.dialog.StorageExceptionDialogFragment
import dev.arkbuilders.navigator.presentation.dialog.edittags.EditTagsDialogFragment
import dev.arkbuilders.navigator.presentation.navigation.Screens
import dev.arkbuilders.navigator.presentation.screen.gallery.GalleryFragment
import dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain.GallerySideEffect
import dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain.GalleryState
import dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain.ProgressState
import dev.arkbuilders.navigator.presentation.screen.main.MainActivity
import dev.arkbuilders.navigator.presentation.utils.FullscreenHelper
import dev.arkbuilders.navigator.presentation.utils.extra.ExtraLoader
import dev.arkbuilders.navigator.presentation.utils.makeGone
import dev.arkbuilders.navigator.presentation.utils.makeVisible
import dev.arkbuilders.navigator.presentation.view.DefaultPopup
import dev.arkbuilders.navigator.presentation.view.DepthPageTransformer
import dev.arkbuilders.navigator.presentation.view.StackedToasts
import org.orbitmvi.orbit.viewmodel.observe
import timber.log.Timber
import java.nio.file.Path
import javax.inject.Inject
import kotlin.system.measureTimeMillis

class GalleryUpliftFragment : Fragment() {
    private val binding by viewBinding(FragmentGalleryBinding::bind)

    @Inject
    lateinit var factory: GalleryUpliftViewModelFactory.Factory
    private val viewModel: GalleryUpliftViewModel by viewModels {
        factory.create(
            startPos = requireArguments().getInt(START_AT_KEY),
            selectingEnabled = requireArguments().getBoolean(SELECTING_ENABLED_KEY),
            selectedResources = requireArguments()
                .getParcelableArray(SELECTED_RESOURCES_KEY)!!
                .toList() as List<ResourceId>,
            rootAndFav = requireArguments()[ROOT_AND_FAV_KEY] as RootAndFav,
            resourcesIds = requireArguments().getParcelableArray(RESOURCES_KEY)!!
                .toList() as List<ResourceId>,
        ).apply {
            App.instance.appComponent.inject(this@GalleryUpliftFragment)
        }
    }

    private val scoreWidget by lazy {
        ScoreWidget(viewModel.scoreWidgetController, viewLifecycleOwner)
    }

    private lateinit var stackedToasts: StackedToasts
    private lateinit var pagerAdapter: PreviewsPagerUplift

    // avoid pointless update on render if there are no changes
    private var cacheTags: Tags? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_gallery, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scoreWidget.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.d(LogTags.GALLERY_SCREEN, "view created in GalleryFragment")
        App.instance.appComponent.inject(this)
        super.onViewCreated(view, savedInstanceState)
        Timber.d(
            LogTags.GALLERY_SCREEN,
            "currentItem = ${binding.viewPager.currentItem}"
        )
        animatePagerAppearance()
        initResultListener()
        stackedToasts = StackedToasts(binding.rvToasts, lifecycleScope)

        FullscreenHelper.setStatusBarVisibility(false, requireActivity().window)
        (requireActivity() as MainActivity).setBottomNavigationVisibility(false)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            onBackClick()
        }
        pagerAdapter =
            PreviewsPagerUplift(lifecycleScope, requireContext(), viewModel)

        initViewPager()
        scoreWidget.init(ScoreWidgetBinding.bind(binding.scoreWidget))
        binding.apply {
            val selectingEnabled =
                requireArguments().getBoolean(GalleryFragment.SELECTING_ENABLED_KEY)
            layoutSelected.isVisible = selectingEnabled
            fabStartSelect.isVisible = !selectingEnabled
            removeResourceFab.setOnClickListener {
                Toast.makeText(
                    requireContext(),
                    "Press and hold to delete",
                    Toast.LENGTH_SHORT
                ).show()
            }
            removeResourceFab.setOnLongClickListener {
                val time = measureTimeMillis {
                    viewModel.onRemoveFabClick()
                }
                Timber.tag(LogTags.GALLERY_SCREEN).d("${time / 1000L}s")
                true
            }
            infoResourceFab.setOnClickListener {
                viewModel.onInfoFabClick()
            }
            shareResourceFab.setOnClickListener {
                viewModel.onShareFabClick()
            }
            fabStartSelect.setOnClickListener {
                viewModel.onSelectingChanged(true)
            }

            openResourceFab.setOnClickListener {
                viewModel.onOpenFabClick()
            }

            editResourceFab.setOnClickListener {
                viewModel.onEditFabClick()
            }

            layoutSelected.setOnClickListener {
                viewModel.onSelectBtnClick()
            }

            layoutSelected.setOnLongClickListener {
                viewModel.onSelectingChanged(false)
                return@setOnLongClickListener true
            }
        }
        viewModel.observe(
            lifecycleOwner = this,
            state = ::render,
            sideEffect = ::handleSideEffect
        )
    }

    private fun handleSideEffect(sideEffect: GallerySideEffect) {
        with(sideEffect) {
            when (this) {
                is GallerySideEffect.ScrollToPage -> {
                    binding.viewPager.adapter?.itemCount?.let { count ->
                        if (this.pos < count) {
                            binding.viewPager.setCurrentItem(
                                this.pos,
                                false
                            )
                        }
                    }
                }

                is GallerySideEffect.DeleteResource -> deleteResource(pos)

                is GallerySideEffect.DisplayStorageException ->
                    displayStorageException(
                        label = label,
                        msg = messenger
                    )

                is GallerySideEffect.EditResource -> editResource(path)
                GallerySideEffect.NavigateBack -> onBackClick()
                GallerySideEffect.NotifyCurrentItemChange ->
                    notifyCurrentItemChanged()

                GallerySideEffect.NotifyResourceChange -> notifyResourcesChanged()
                GallerySideEffect.NotifyResourceScoresChanged ->
                    notifyResourceScoresChanged()

                GallerySideEffect.NotifyTagsChanged -> notifyTagsChanged()
                is GallerySideEffect.OpenLink -> openLink(url)

                is GallerySideEffect.ShareLink -> shareLink(url)
                is GallerySideEffect.ShareResource -> shareResource(path)
                is GallerySideEffect.ShowEditTagsDialog -> showEditTagsDialog(
                    resource = resource,
                    resources = resources,
                    statsStorage = statsStorage,
                    rootAndFav = rootAndFav,
                    index = index,
                    storage = storage,
                )

                is GallerySideEffect.ShowInfoAlert -> showInfoAlert(
                    path = path,
                    resource = resource,
                    metadata = metadata
                )

                is GallerySideEffect.ToastIndexFailedPath -> toastIndexFailedPath(
                    path
                )

                is GallerySideEffect.ViewInExternalApp -> viewInExternalApp(path)
                is GallerySideEffect.AbortSelectAnimation ->
                    binding.cbSelected.jumpDrawablesToCurrentState()
            }
        }
    }

    private fun render(state: GalleryState) {
        pagerAdapter.dispatchUpdates(state.galleryItems)
        setControlsVisibility(state.controlsVisible)
        toggleSelecting(state.selectingEnabled)
        displaySelected(
            state.currentItemSelected,
            state.selectedResources.size,
            state.galleryItems.size
        )
        handleProgressState(state.progressState)
        if (state.tags != cacheTags) {
            displayPreviewTags(state.currentItem.id(), state.tags)
            cacheTags = state.tags
        }
        setupPreview(state.currentPos, state.currentItem.metadata)
    }

    private fun onBackClick() {
        Timber.d(LogTags.GALLERY_SCREEN, "quitting from GalleryPresenter")
        notifySelectedChanged(viewModel.container.stateFlow.value.selectedResources)
        exitFullscreen()
        viewModel.router.exit()
    }

    private fun setupPreview(
        pos: Int,
        meta: Metadata
    ) {
        with(binding) {
            setupOpenEditFABs(meta)
            ExtraLoader.load(
                meta,
                listOf(primaryExtra, secondaryExtra),
                verbose = true
            )
            requireArguments().putInt(START_AT_KEY, pos)
        }
    }

    private fun setControlsVisibility(visible: Boolean) {
        binding.previewControls.isVisible = visible
    }

    private fun editResource(resourcePath: Path) {
        val intent = getExternalAppIntent(
            resourcePath,
            Intent.ACTION_EDIT,
            false /* don't detach to get the result */
        )
        intent.apply {
            putExtra("SAVE_FOLDER_PATH", resourcePath.parent.toString())
            putExtra("real_file_path_2", resourcePath.toString())
        }
        try {
            requireContext().startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                requireContext(), getString(R.string.no_app_found_to_open_this_file),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun shareResource(resourcePath: Path) =
        openIntentChooser(
            resourcePath,
            Intent.ACTION_SEND,
            detachProcess = false
        )

    private fun openLink(link: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        val uri = Uri.parse(link)
        intent.data = uri
        startActivity(Intent.createChooser(intent, "View the link with:"))
    }

    private fun shareLink(link: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, link)
        intent.type = "text/plain"
        startActivity(Intent.createChooser(intent, "Share the link with:"))
    }

    private fun showInfoAlert(path: Path, resource: Resource, metadata: Metadata) {
        DetailsAlertDialog(path, resource, metadata, requireContext()).show()
    }

    private fun viewInExternalApp(resourcePath: Path) {
        openIntentChooser(resourcePath, Intent.ACTION_VIEW, true)
    }

    private fun deleteResource(pos: Int) {
        binding.viewPager.apply {
            setPageTransformer(null)
            pagerAdapter.notifyItemRemoved(pos)
            doOnNextLayout {
                setPageTransformer(DepthPageTransformer())
            }
        }
    }

    private fun displayStorageException(label: String, msg: String) {
        StorageExceptionDialogFragment.newInstance(label, msg).show(
            childFragmentManager,
            StorageExceptionDialogFragment.TAG
        )
    }

    private fun notifyResourcesChanged() {
        setFragmentResult(REQUEST_RESOURCES_CHANGED_KEY, bundleOf())
    }

    private fun notifyTagsChanged() {
        setFragmentResult(REQUEST_TAGS_CHANGED_KEY, bundleOf())
    }

    private fun notifyResourceScoresChanged() {
        setFragmentResult(SCORES_CHANGED_KEY, bundleOf())
    }

    private fun notifySelectedChanged(
        selected: List<ResourceId>
    ) {
        setFragmentResult(
            SELECTED_CHANGED_KEY,
            bundleOf().apply {
                putBoolean(
                    SELECTING_ENABLED_KEY,
                    requireArguments().getBoolean(SELECTING_ENABLED_KEY)
                )
                putParcelableArray(
                    SELECTED_RESOURCES_KEY,
                    selected.toTypedArray()
                )
            }
        )
    }

    private fun toastIndexFailedPath(path: Path) {
        stackedToasts.toast(path)
    }

    private fun displayPreviewTags(resource: ResourceId, tags: Tags) {
        Timber.d(
            LogTags.GALLERY_SCREEN,
            "displaying tags of resource $resource for preview"
        )
        binding.tagsCg.removeAllViews()

        tags.forEach { tag ->
            val chip = Chip(context)
            chip.text = tag

            chip.setOnClickListener {
                showTagMenuPopup(tag, chip)
            }
            binding.tagsCg.addView(chip)
        }

        binding.tagsCg.addView(createEditChip())
    }

    private fun showEditTagsDialog(
        resource: ResourceId,
        rootAndFav: RootAndFav,
        resources: List<ResourceId>,
        index: ResourceIndex,
        storage: TagStorage,
        statsStorage: StatsStorage
    ) {
        Timber.d(
            LogTags.GALLERY_SCREEN,
            "showing [edit-tags] dialog for resource $resource"
        )
        val dialog = EditTagsDialogFragment.newInstance(
            rootAndFav = rootAndFav,
            resources = resources,
            index = index,
            storage = storage,
            statsStorage = statsStorage
        )
        dialog.show(childFragmentManager, EditTagsDialogFragment.FRAGMENT_TAG)
    }

    private fun handleProgressState(state: ProgressState) {
        when (state) {
            ProgressState.HideProgress -> setProgressVisibility(
                false,
                ""
            )

            ProgressState.Indexing -> setProgressVisibility(
                true,
                getString(R.string.progress_text_changes_detected_indexing)
            )

            ProgressState.ProvidingDataStorage -> setProgressVisibility(
                true,
                getString(R.string.progress_text_providing_data_storage)
            )

            ProgressState.ProvidingMetaDataStorage -> setProgressVisibility(
                true,
                getString(R.string.progress_text_providing_metadata_storage)
            )

            ProgressState.ProvidingPreviewStorage -> setProgressVisibility(
                true,
                getString(R.string.progress_text_providing_previews_storage)
            )

            ProgressState.ProvidingRootIndex -> setProgressVisibility(
                true,
                getString(R.string.progress_text_providing_root_index)
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setProgressVisibility(isVisible: Boolean, withText: String) {
        binding.layoutProgress.apply {
            root.isVisible = isVisible

            if (isVisible) {
                root.setOnTouchListener { _, _ ->
                    return@setOnTouchListener true
                }
            } else {
                root.setOnTouchListener(null)
            }

            if (withText.isNotEmpty()) {
                progressText.setVisibilityAndLoadingStatus(View.VISIBLE)
                progressText.loadingText = withText
            } else {
                progressText.setVisibilityAndLoadingStatus(View.GONE)
            }
        }
    }

    private fun exitFullscreen() {
        FullscreenHelper.setStatusBarVisibility(true, requireActivity().window)
        (requireActivity() as MainActivity).setBottomNavigationVisibility(true)
    }

    private fun notifyCurrentItemChanged() {
        binding.viewPager.post {
            pagerAdapter.notifyItemChanged(binding.viewPager.currentItem)
        }
    }

    private fun displaySelected(
        selected: Boolean,
        selectedCount: Int,
        itemCount: Int
    ) = with(binding) {
        Timber.d("display ${System.currentTimeMillis()}")
        cbSelected.isChecked = selected
        tvSelectedOf.text = "$selectedCount/$itemCount"

        return@with
    }

    private fun toggleSelecting(enabled: Boolean) {
        binding.layoutSelected.isVisible = enabled
        binding.fabStartSelect.isVisible = !enabled
        requireArguments().apply {
            putBoolean(GalleryFragment.SELECTING_ENABLED_KEY, enabled)
        }
    }

    private fun setupOpenEditFABs(meta: Metadata?) = binding.apply {
        openResourceFab.makeGone()
        editResourceFab.makeGone()
        when (meta) {
            is Metadata.Video, is Metadata.Link, null -> {
                // "open" capabilities only
                openResourceFab.makeVisible()
            }

            is Metadata.Document, is Metadata.PlainText -> {
                // both "open" and "edit" capabilities
                editResourceFab.makeVisible()
                openResourceFab.makeVisible()
            }

            is Metadata.Image -> {
                // "edit" capabilities only
                editResourceFab.makeVisible()
            }

            else -> {}
        }
    }

    /**
     * setFragmentResult notifies ResourcesFragment
     * It is duplicated since the result can only be consumed once
     */
    private fun initResultListener() {
        childFragmentManager.setFragmentResultListener(
            EditTagsDialogFragment.REQUEST_TAGS_CHANGED_KEY,
            this
        ) { _, _ ->
            setFragmentResult(REQUEST_TAGS_CHANGED_KEY, bundleOf())
            viewModel.onTagsChanged()
        }

        childFragmentManager.setFragmentResultListener(
            StorageExceptionDialogFragment.STORAGE_CORRUPTION_DETECTED,
            this
        ) { _, _ ->
            viewModel.router.newRootScreen(Screens.FoldersScreen())
        }
    }

    private fun animatePagerAppearance() {
        binding.viewPager.animate().apply {
            duration = 500L
            alpha(1f)
        }
    }

    private fun initViewPager() = with(binding.viewPager) {
        adapter = pagerAdapter
        offscreenPageLimit = 2
        val rv = (getChildAt(0) as RecyclerView)
        (rv.itemAnimator as SimpleItemAnimator).removeDuration = 0
        setPageTransformer(DepthPageTransformer())

        registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.onPageChanged(position)
            }
        })
    }

    private fun showTagMenuPopup(tag: Tag, tagView: View) {
        val menuBinding = PopupGalleryTagMenuBinding
            .inflate(requireActivity().layoutInflater)
        val popup = DefaultPopup(
            menuBinding,
            R.style.BottomFadeScaleAnimation,
            R.drawable.bg_rounded_16dp
        )
        menuBinding.apply {
            btnNewSelection.setOnClickListener {
                viewModel.onTagSelected(tag)
                popup.popupWindow.dismiss()
            }
            btnRemoveTag.setOnClickListener {
                viewModel.onTagRemove(tag)
                popup.popupWindow.dismiss()
            }
        }
        popup.showAbove(tagView)
    }

    private fun openIntentChooser(
        resourcePath: Path,
        actionType: String,
        detachProcess: Boolean
    ) {
        Timber.i(
            LogTags.GALLERY_SCREEN,
            "Opening resource in an external application " +
                "path: $resourcePath" +
                "action: $actionType"
        )

        val intent = getExternalAppIntent(resourcePath, actionType, detachProcess)
        val title = when (actionType) {
            Intent.ACTION_VIEW -> "View the resource with:"
            Intent.ACTION_EDIT -> "Edit the resource with:"
            Intent.ACTION_SEND -> "Share the resource with:"
            else -> "Open the resource with:"
        }
        startActivity(Intent.createChooser(intent, title))
    }

    private fun getExternalAppIntent(
        resourcePath: Path,
        actionType: String,
        detachProcess: Boolean
    ): Intent {
        val file = resourcePath.toFile()
        val extension: String = extension(resourcePath)
        val uri = FileProvider.getUriForFile(
            requireContext(),
            BuildConfig.APPLICATION_ID + ".provider",
            file
        )
        val intent = Intent().apply {
            setDataAndType(uri, requireContext().contentResolver.getType(uri))
            action = actionType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            when (actionType) {
                Intent.ACTION_EDIT -> {
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }

                Intent.ACTION_SEND -> {
                    putExtra(Intent.EXTRA_STREAM, uri)
                }
            }
            if (detachProcess) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        Timber.d(
            LogTags.GALLERY_SCREEN,
            "URI: ${intent.data}" + "MIME: ${intent.type}"
        )
        return intent
    }

    private fun getPXFromDP(dpValue: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dpValue,
            resources.displayMetrics
        )
    }

    private fun createEditChip(): Chip {
        return Chip(context).also {
            it.apply {
                setChipIconResource(R.drawable.ic_baseline_edit_24)
                chipBackgroundColor =
                    requireActivity().getColorStateList(R.color.colorPrimary)
                chipStartPadding = getPXFromDP(12f)
                chipEndPadding = getPXFromDP(12f)
                textStartPadding = 0f
                textEndPadding = 0f

                setOnClickListener {
                    val position = binding.viewPager.currentItem
                    Timber.d(
                        LogTags.GALLERY_SCREEN,
                        "[edit_tags] clicked at position $position"
                    )
                    viewModel.onEditTagsDialogBtnClick()
                }
            }
        }
    }

    companion object {
        private const val ROOT_AND_FAV_KEY = "rootAndFav"
        private const val RESOURCES_KEY = "resources"
        private const val START_AT_KEY = "startAt"
        const val SELECTING_ENABLED_KEY = "selectingEnabled"
        const val SELECTED_RESOURCES_KEY = "selectedResources"
        const val REQUEST_TAGS_CHANGED_KEY = "tagsChangedGallery"
        const val REQUEST_RESOURCES_CHANGED_KEY = "resourcesChangedGallery"
        const val SCORES_CHANGED_KEY = "scoresChangedInGallery"
        const val SELECTED_CHANGED_KEY = "selectedChanged"

        fun newInstance(
            rootAndFav: RootAndFav,
            resources: List<ResourceId>,
            startAt: Int,
            selectingEnabled: Boolean = false,
            selectedResources: List<ResourceId> = emptyList(),
        ) = GalleryUpliftFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ROOT_AND_FAV_KEY, rootAndFav)
                putParcelableArray(RESOURCES_KEY, resources.toTypedArray())
                putInt(START_AT_KEY, startAt)
                putBoolean(SELECTING_ENABLED_KEY, selectingEnabled)
                putParcelableArray(
                    SELECTED_RESOURCES_KEY,
                    selectedResources.toTypedArray()
                )
            }
        }
    }
}
