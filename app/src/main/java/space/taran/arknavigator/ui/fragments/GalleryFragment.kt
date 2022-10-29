package space.taran.arknavigator.ui.fragments

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import moxy.MvpAppCompatFragment
import moxy.ktx.moxyPresenter
import space.taran.arknavigator.BuildConfig
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.FragmentGalleryBinding
import space.taran.arknavigator.databinding.PopupGalleryTagMenuBinding
import space.taran.arknavigator.mvp.model.repo.RootAndFav
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.kind.ResourceKind
import space.taran.arknavigator.mvp.presenter.GalleryPresenter
import space.taran.arknavigator.mvp.view.GalleryView
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.ui.activity.MainActivity
import space.taran.arknavigator.ui.adapter.previewpager.PreviewsPager
import space.taran.arknavigator.ui.extra.ExtraLoader
import space.taran.arknavigator.ui.fragments.dialog.DetailsAlertDialog
import space.taran.arknavigator.ui.fragments.dialog.EditTagsDialogFragment
import space.taran.arknavigator.ui.view.DefaultPopup
import space.taran.arknavigator.ui.view.DepthPageTransformer
import space.taran.arknavigator.ui.view.StackedToasts
import space.taran.arknavigator.utils.FullscreenHelper
import space.taran.arknavigator.utils.LogTags.GALLERY_SCREEN
import space.taran.arknavigator.utils.Tag
import space.taran.arknavigator.utils.Tags
import space.taran.arknavigator.utils.extension
import space.taran.arknavigator.utils.extensions.makeGone
import space.taran.arknavigator.utils.extensions.makeVisible
import java.nio.file.Path

class GalleryFragment : MvpAppCompatFragment(), GalleryView {

    private lateinit var binding: FragmentGalleryBinding

    private val presenter by moxyPresenter {
        GalleryPresenter(
            requireArguments()[ROOT_AND_FAV_KEY] as RootAndFav,
            requireArguments().getLongArray(RESOURCES_KEY)!!.toList(),
            requireArguments().getInt(START_AT_KEY)
        ).apply {
            Log.d(GALLERY_SCREEN, "creating GalleryPresenter")
            App.instance.appComponent.inject(this)
        }
    }
    private lateinit var stackedToasts: StackedToasts
    private lateinit var pagerAdapter: PreviewsPager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGalleryBinding.inflate(inflater, container, false)
        Log.d(GALLERY_SCREEN, "inflating layout for GalleryFragment")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(GALLERY_SCREEN, "view created in GalleryFragment")
        super.onViewCreated(view, savedInstanceState)
        App.instance.appComponent.inject(this)
    }

    override fun init() {
        Log.d(GALLERY_SCREEN, "currentItem = ${binding.viewPager.currentItem}")

        animatePagerAppearance()
        initResultListener()
        stackedToasts = StackedToasts(binding.rvToasts, lifecycleScope)

        FullscreenHelper.setStatusBarVisibility(false, requireActivity().window)
        (requireActivity() as MainActivity).setBottomNavigationVisibility(false)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            presenter.onBackClick()
        }

        pagerAdapter = PreviewsPager(presenter)

        initViewPager()

        binding.apply {
            removeResourceFab.setOnLongClickListener {
                presenter.onRemoveFabClick()
                true
            }

            infoResourceFab.setOnClickListener {
                presenter.onInfoFabClick()
            }
            shareResourceFab.setOnClickListener {
                presenter.onShareFabClick()
            }

            openResourceFab.setOnClickListener {
                presenter.onOpenFabClick()
            }

            editResourceFab.setOnClickListener {
                presenter.onEditFabClick()
            }
        }
    }

    override fun updatePagerAdapter() {
        pagerAdapter.notifyDataSetChanged()
        binding.viewPager.setCurrentItem(
            requireArguments().getInt(START_AT_KEY),
            false
        )
    }

    override fun updatePagerAdapterWithDiff() {
        presenter.diffResult?.dispatchUpdatesTo(pagerAdapter)
    }

    override fun setupPreview(
        pos: Int,
        resource: ResourceMeta,
        filePath: String
    ) {
        lifecycleScope.launch {
            with(binding) {
                setupOpenEditFABs(resource.kind)
                ExtraLoader.load(
                    resource,
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
                requireContext(), getString(R.string.no_app_found_to_open_this_file),
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

    override fun showInfoAlert(path: Path, resourceMeta: ResourceMeta) {
        DetailsAlertDialog(path, resourceMeta, requireContext()).show()
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

    override fun notifyResourcesChanged() {
        setFragmentResult(REQUEST_RESOURCES_CHANGED_KEY, bundleOf())
    }

    override fun notifyTagsChanged() {
        setFragmentResult(REQUEST_TAGS_CHANGED_KEY, bundleOf())
    }

    override fun toastIndexFailedPath(path: Path) {
        stackedToasts.toast(path)
    }

    override fun displayPreviewTags(resource: ResourceId, tags: Tags) {
        lifecycleScope.launch {
            Log.d(
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
        Log.d(
            GALLERY_SCREEN,
            "showing [edit-tags] dialog for resource $resource"
        )
        val dialog = EditTagsDialogFragment.newInstance(
            requireArguments()[ROOT_AND_FAV_KEY] as RootAndFav,
            listOf(resource),
            presenter.index,
            presenter.storage,
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
        pagerAdapter.notifyItemChanged(binding.viewPager.currentItem)
    }

    override fun onResume() {
        super.onResume()
        presenter.onResume()
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
    }

    private fun animatePagerAppearance() {
        binding.viewPager.animate().apply {
            duration = 500L
            alpha(1f)
        }
    }

    private fun setupOpenEditFABs(kind: ResourceKind?) = binding.apply {
        openResourceFab.makeGone()
        editResourceFab.makeGone()
        when (kind) {
            is ResourceKind.Video, is ResourceKind.Link, null -> {
                // "open" capabilities only
                openResourceFab.makeVisible()
            }
            is ResourceKind.Document, is ResourceKind.PlainText -> {
                // both "open" and "edit" capabilities
                editResourceFab.makeVisible()
                openResourceFab.makeVisible()
            }
            is ResourceKind.Image -> {
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
        Log.i(GALLERY_SCREEN, "Opening resource in an external application")
        Log.i(GALLERY_SCREEN, "path: $resourcePath")
        Log.i(GALLERY_SCREEN, "action: $actionType")

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
        Log.d(GALLERY_SCREEN, "URI: ${intent.data}")
        Log.d(GALLERY_SCREEN, "MIME: ${intent.type}")
        return intent
    }

    private fun getPXFromDP(dpValue: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dpValue,
            getResources().displayMetrics
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
                    Log.d(
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
        const val REQUEST_TAGS_CHANGED_KEY = "tagsChangedGallery"
        const val REQUEST_RESOURCES_CHANGED_KEY = "resourcesChangedGallery"

        fun newInstance(
            rootAndFav: RootAndFav,
            resources: List<ResourceId>,
            startAt: Int
        ) =
            GalleryFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ROOT_AND_FAV_KEY, rootAndFav)
                    putLongArray(RESOURCES_KEY, resources.toLongArray())
                    putInt(START_AT_KEY, startAt)
                }
            }
    }
}
