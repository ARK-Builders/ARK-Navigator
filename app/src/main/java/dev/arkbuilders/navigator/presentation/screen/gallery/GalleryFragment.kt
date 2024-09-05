package dev.arkbuilders.navigator.presentation.screen.gallery

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.viewpager2.widget.ViewPager2
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.chip.Chip
import dev.arkbuilders.components.databinding.ScoreWidgetBinding
import dev.arkbuilders.components.scorewidget.ScoreWidget
import dev.arkbuilders.navigator.BuildConfig
import dev.arkbuilders.navigator.R
import dev.arkbuilders.navigator.data.utils.LogTags.GALLERY_SCREEN
import dev.arkbuilders.navigator.databinding.FragmentGalleryBinding
import dev.arkbuilders.navigator.databinding.PopupGalleryTagMenuBinding
import dev.arkbuilders.navigator.presentation.App
import dev.arkbuilders.navigator.presentation.dialog.DetailsAlertDialog
import dev.arkbuilders.navigator.presentation.dialog.StorageExceptionDialogFragment
import dev.arkbuilders.navigator.presentation.dialog.edittags.EditTagsDialogFragment
import dev.arkbuilders.navigator.presentation.navigation.Screens
import dev.arkbuilders.navigator.presentation.screen.gallery.previewpager.PreviewsPager
import dev.arkbuilders.navigator.presentation.screen.main.MainActivity
import dev.arkbuilders.navigator.presentation.utils.FullscreenHelper
import dev.arkbuilders.navigator.presentation.utils.extra.ExtraLoader
import dev.arkbuilders.navigator.presentation.utils.makeGone
import dev.arkbuilders.navigator.presentation.utils.makeVisible
import dev.arkbuilders.navigator.presentation.view.DefaultPopup
import dev.arkbuilders.navigator.presentation.view.DepthPageTransformer
import dev.arkbuilders.navigator.presentation.view.StackedToasts
import kotlinx.coroutines.launch
import moxy.MvpAppCompatFragment
import moxy.ktx.moxyPresenter
import dev.arkbuilders.arkfilepicker.folders.RootAndFav
import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.data.index.Resource
import dev.arkbuilders.arklib.data.meta.Metadata
import dev.arkbuilders.arklib.user.tags.Tag
import dev.arkbuilders.arklib.user.tags.Tags
import dev.arkbuilders.arklib.utils.extension
import timber.log.Timber
import java.nio.file.Path
import kotlin.system.measureTimeMillis

class GalleryFragment :
    MvpAppCompatFragment(R.layout.fragment_gallery), GalleryView {

    private val binding by viewBinding(FragmentGalleryBinding::bind)

    private val presenter by moxyPresenter {
        GalleryPresenter(
            rootAndFav = requireArguments()[ROOT_AND_FAV_KEY] as RootAndFav,
            resourcesIds = requireArguments().getParcelableArray(RESOURCES_KEY)!!
                .toList() as List<ResourceId>,
            startAt = requireArguments().getInt(START_AT_KEY),
            selectingEnabled = requireArguments().getBoolean(SELECTING_ENABLED_KEY),
            selectedResources = (
                requireArguments().getParcelableArray(SELECTED_RESOURCES_KEY)!!
                    .toList() as List<ResourceId>
                )
                .toMutableList()
        ).apply {

            Timber.d(GALLERY_SCREEN, "creating GalleryPresenter")

            App.instance.appComponent.inject(this)
        }
    }
    private lateinit var stackedToasts: StackedToasts
    private lateinit var pagerAdapter: PreviewsPager
    private val scoreWidget by lazy {
        ScoreWidget(presenter.scoreWidgetController, viewLifecycleOwner)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.d(GALLERY_SCREEN, "view created in GalleryFragment")
        super.onViewCreated(view, savedInstanceState)
        App.instance.appComponent.inject(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scoreWidget.onDestroyView()
    }

    override fun init() {
        Timber.d(GALLERY_SCREEN, "currentItem = ${binding.viewPager.currentItem}")

        animatePagerAppearance()
        initResultListener()
        stackedToasts = StackedToasts(binding.rvToasts, lifecycleScope)

        FullscreenHelper.setStatusBarVisibility(false, requireActivity().window)
        (requireActivity() as MainActivity).setBottomNavigationVisibility(false)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            presenter.onBackClick()
        }

        pagerAdapter = PreviewsPager(requireContext(), presenter)

        initViewPager()
        scoreWidget.init(ScoreWidgetBinding.bind(binding.scoreWidget))

        binding.apply {
            val selectingEnabled =
                requireArguments().getBoolean(SELECTING_ENABLED_KEY)
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
                    presenter.onRemoveFabClick()
                }
                Timber.tag(GALLERY_SCREEN).d("${time / 1000L}s")
                true
            }

            infoResourceFab.setOnClickListener {
                presenter.onInfoFabClick()
            }
            shareResourceFab.setOnClickListener {
                presenter.onShareFabClick()
            }
            fabStartSelect.setOnClickListener {
                presenter.onSelectingChanged()
            }

            openResourceFab.setOnClickListener {
                presenter.onOpenFabClick()
            }

            editResourceFab.setOnClickListener {
                presenter.onEditFabClick()
            }

            layoutSelected.setOnClickListener {
                presenter.onSelectBtnClick()
            }

            layoutSelected.setOnLongClickListener {
                presenter.onSelectingChanged()
                return@setOnLongClickListener true
            }
        }
    }

    override fun updatePagerAdapter() {
        pagerAdapter.notifyDataSetChanged()
        binding.viewPager.adapter?.itemCount?.let { count ->
            val startAt = requireArguments().getInt(START_AT_KEY)
            if (startAt < count) {
                binding.viewPager.setCurrentItem(
                    startAt,
                    false
                )
            }
        }
    }

    override fun updatePagerAdapterWithDiff() {
        presenter.diffResult?.dispatchUpdatesTo(pagerAdapter)
    }

    override fun setupPreview(
        pos: Int,
        meta: Metadata
    ) {
        lifecycleScope.launch {
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
    }

    override fun setPreviewsScrollingEnabled(enabled: Boolean) {
        binding.viewPager.isUserInputEnabled = enabled
    }

    override fun setControlsVisibility(visible: Boolean) {
        binding.previewControls.isVisible = visible
    }

    override fun editResource(resourcePath: Path) {
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
                requireContext(),
                getString(R.string.no_app_found_to_open_this_file),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun shareResource(resourcePath: Path) =
        openIntentChooser(
            resourcePath,
            Intent.ACTION_SEND,
            detachProcess = false
        )

    override fun openLink(link: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        val uri = Uri.parse(link)
        intent.data = uri
        startActivity(Intent.createChooser(intent, "View the link with:"))
    }

    override fun shareLink(link: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, link)
        intent.type = "text/plain"
        startActivity(Intent.createChooser(intent, "Share the link with:"))
    }

    override fun showInfoAlert(path: Path, resource: Resource, metadata: Metadata) {
        DetailsAlertDialog(path, resource, metadata, requireContext()).show()
    }

    override fun viewInExternalApp(resourcePath: Path) {
        openIntentChooser(resourcePath, Intent.ACTION_VIEW, true)
    }

    override fun deleteResource(pos: Int) {
        binding.viewPager.apply {
            setPageTransformer(null)
            pagerAdapter.notifyItemRemoved(pos)
            doOnNextLayout {
                setPageTransformer(DepthPageTransformer())
            }
        }
    }

    override fun displayStorageException(label: String, msg: String) {
        StorageExceptionDialogFragment.newInstance(label, msg).show(
            childFragmentManager,
            StorageExceptionDialogFragment.TAG
        )
    }

    override fun notifyResourcesChanged() {
        setFragmentResult(REQUEST_RESOURCES_CHANGED_KEY, bundleOf())
    }

    override fun notifyTagsChanged() {
        setFragmentResult(REQUEST_TAGS_CHANGED_KEY, bundleOf())
    }

    override fun notifyResourceScoresChanged() {
        setFragmentResult(SCORES_CHANGED_KEY, bundleOf())
    }

    override fun notifySelectedChanged(
        selected: List<ResourceId>
    ) {
        setFragmentResult(
            SELECTED_CHANGED_KEY,
            bundleOf().apply {
                putBoolean(
                    SELECTING_ENABLED_KEY,
                    requireArguments().getBoolean(SELECTING_ENABLED_KEY)
                )
                putParcelableArray(SELECTED_RESOURCES_KEY, selected.toTypedArray())
            }
        )
    }

    override fun toastIndexFailedPath(path: Path) {
        stackedToasts.toast(path)
    }

    override fun displayPreviewTags(resource: ResourceId, tags: Tags) {
        lifecycleScope.launch {
            Timber.d(
                GALLERY_SCREEN,
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
    }

    override fun showEditTagsDialog(
        resource: ResourceId
    ) {
        Timber.d(
            GALLERY_SCREEN,
            "showing [edit-tags] dialog for resource $resource"
        )
        val dialog = EditTagsDialogFragment.newInstance(
            requireArguments()[ROOT_AND_FAV_KEY] as RootAndFav,
            listOf(resource),
            presenter.index,
            presenter.tagsStorage,
            presenter.statsStorage
        )
        dialog.show(childFragmentManager, EditTagsDialogFragment.FRAGMENT_TAG)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun setProgressVisibility(isVisible: Boolean, withText: String) {
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

    override fun exitFullscreen() {
        FullscreenHelper.setStatusBarVisibility(true, requireActivity().window)
        (requireActivity() as MainActivity).setBottomNavigationVisibility(true)
    }

    override fun notifyCurrentItemChanged() {
        binding.viewPager.post {
            pagerAdapter.notifyItemChanged(binding.viewPager.currentItem)
        }
    }

    override fun displaySelected(
        selected: Boolean,
        showAnim: Boolean,
        selectedCount: Int,
        itemCount: Int
    ) = with(binding) {
        Timber.d("display ${System.currentTimeMillis()}")
        cbSelected.isChecked = selected
        if (!showAnim) {
            cbSelected.jumpDrawablesToCurrentState()
        }
        tvSelectedOf.text = "$selectedCount/$itemCount"

        return@with
    }

    override fun onResume() {
        super.onResume()
        presenter.onResume()
    }

    override fun toggleSelecting(enabled: Boolean) {
        binding.layoutSelected.isVisible = enabled
        binding.fabStartSelect.isVisible = !enabled
        requireArguments().apply {
            putBoolean(SELECTING_ENABLED_KEY, enabled)
        }
        if (enabled) {
            presenter.onSelectBtnClick()
        } else {
            binding.cbSelected.isChecked = false
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
                presenter.onPageChanged(position)
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
                presenter.onTagSelected(tag)
                popup.popupWindow.dismiss()
            }
            btnRemoveTag.setOnClickListener {
                presenter.onTagRemove(tag)
                popup.popupWindow.dismiss()
            }
        }
        popup.showAbove(tagView)
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
            presenter.onTagsChanged()
        }

        childFragmentManager.setFragmentResultListener(
            StorageExceptionDialogFragment.STORAGE_CORRUPTION_DETECTED,
            this
        ) { _, _ ->
            presenter.router.newRootScreen(Screens.FoldersScreen())
        }
    }

    private fun animatePagerAppearance() {
        binding.viewPager.animate().apply {
            duration = 500L
            alpha(1f)
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

    private fun openIntentChooser(
        resourcePath: Path,
        actionType: String,
        detachProcess: Boolean
    ) {
        Timber.i(
            GALLERY_SCREEN,
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
            GALLERY_SCREEN,
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
                        GALLERY_SCREEN,
                        "[edit_tags] clicked at position $position"
                    )
                    presenter.onEditTagsDialogBtnClick()
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
            selectedResources: List<ResourceId> = emptyList()
        ) = GalleryFragment().apply {
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
