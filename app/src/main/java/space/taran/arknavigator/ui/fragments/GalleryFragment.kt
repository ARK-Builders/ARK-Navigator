package space.taran.arknavigator.ui.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.View
import androidx.activity.addCallback
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.chip.Chip
import moxy.MvpAppCompatFragment
import moxy.ktx.moxyPresenter
import space.taran.arknavigator.BuildConfig
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.FragmentGalleryBinding
import space.taran.arknavigator.mvp.model.repo.RootAndFav
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceKind
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.presenter.GalleryPresenter
import space.taran.arknavigator.mvp.view.GalleryView
import space.taran.arknavigator.mvp.view.NotifiableView
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.ui.activity.MainActivity
import space.taran.arknavigator.ui.adapter.PreviewsPager
import space.taran.arknavigator.ui.extra.ExtraLoader
import space.taran.arknavigator.ui.fragments.dialog.EditTagsDialogFragment
import space.taran.arknavigator.ui.fragments.utils.Notifications
import space.taran.arknavigator.ui.view.DepthPageTransformer
import space.taran.arknavigator.utils.FullscreenHelper
import space.taran.arknavigator.utils.GALLERY_SCREEN
import space.taran.arknavigator.utils.Tags
import space.taran.arknavigator.utils.extension
import space.taran.arknavigator.utils.extensions.makeGone
import space.taran.arknavigator.utils.extensions.makeVisible
import java.nio.file.Path

class GalleryFragment : MvpAppCompatFragment(), GalleryView, NotifiableView {

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

        initResultListener()

        FullscreenHelper.setStatusBarVisibility(false, requireActivity().window)
        (requireActivity() as MainActivity).setToolbarVisibility(false)
        (requireActivity() as MainActivity).setBottomNavigationVisibility(false)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            presenter.onBackClick()
        }

        pagerAdapter = PreviewsPager(presenter.previewsPresenter)

        binding.apply {
            viewPager.apply {
                adapter = pagerAdapter
                offscreenPageLimit = 2
                ((getChildAt(0) as RecyclerView).itemAnimator as SimpleItemAnimator).removeDuration = 0
                setPageTransformer(DepthPageTransformer())

                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        presenter.onPageChanged(position)
                    }
                })
            }

            removeResourceFab.setOnLongClickListener {
                presenter.onRemoveFabClick()
                true
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
    }

    override fun setupPreview(pos: Int, resource: ResourceMeta, filePath: String) {
        if (binding.viewPager.currentItem != pos)
            binding.viewPager.setCurrentItem(pos, false)
        setupOpenEditFABs(resource.kind)
        ExtraLoader.load(
            resource,
            listOf(binding.primaryExtra, binding.secondaryExtra),
            verbose = true
        )
        requireArguments().putInt(START_AT_KEY, pos)
    }

    override fun setPreviewsScrollingEnabled(enabled: Boolean) {
        binding.viewPager.isUserInputEnabled = enabled
    }

    override fun setControlsVisibility(visible: Boolean) {
        binding.previewControls.isVisible = visible
    }

    override fun notifyUser(message: String, moreTime: Boolean) {
        Notifications.notifyUser(context, message, moreTime)
    }

    override fun editResource(resourcePath: Path) =
        openIntentChooser(resourcePath, Intent.ACTION_EDIT, detachProcess = true)

    override fun shareResource(resourcePath: Path) =
        openIntentChooser(resourcePath, Intent.ACTION_SEND, detachProcess = false)

    override fun viewInExternalApp(resourcePath: Path) {
        openIntentChooser(resourcePath, Intent.ACTION_VIEW, true)
    }

    override fun deleteResource(pos: Int) {
        binding.viewPager.apply {
            setPageTransformer(null)
            pagerAdapter.removeItem(pos)
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

    override fun displayPreviewTags(resource: ResourceId, tags: Tags) {
        Log.d(GALLERY_SCREEN, "displaying tags of resource $resource for preview")

        binding.tagsCg.removeAllViews()

        tags.forEach { tag ->
            val chip = Chip(context)
            chip.text = tag

            chip.setOnLongClickListener {
                Log.d(GALLERY_SCREEN, "tag $tag on resource $resource long-clicked")
                notifyUser("Tag \"$tag\" removed")
                presenter.onTagRemove(tag)
                true
            }

            binding.tagsCg.addView(chip)
        }

        binding.tagsCg.addView(createEditChip())
    }

    override fun showEditTagsDialog(
        resource: ResourceId
    ) {
        Log.d(GALLERY_SCREEN, "showing [edit-tags] dialog for resource $resource")
        val dialog = EditTagsDialogFragment.newInstance(
            requireArguments()[ROOT_AND_FAV_KEY] as RootAndFav,
            resource,
            presenter.index,
            presenter.storage
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
        (requireActivity() as MainActivity).setToolbarVisibility(true)
        (requireActivity() as MainActivity).setBottomNavigationVisibility(true)
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

    private fun setupOpenEditFABs(kind: ResourceKind?) {
        binding.apply {
            when (kind) {
                ResourceKind.VIDEO -> {
                    // "open" capabilities only
                    editResourceFab.makeGone()
                    openResourceFab.makeVisible()
                }
                ResourceKind.DOCUMENT -> {
                    // both "open" and "edit" capabilities
                    editResourceFab.makeVisible()
                    openResourceFab.makeVisible()
                }
                ResourceKind.IMAGE -> {
                    // "edit" capabilities only
                    openResourceFab.makeGone()
                    editResourceFab.makeVisible()
                }
                null -> {
                    openResourceFab.makeVisible()
                }
            }
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

        val file = resourcePath.toFile()
        val extension: String = extension(resourcePath)

        val context = requireContext()

        val uri = FileProvider.getUriForFile(
            context,
            BuildConfig.APPLICATION_ID + ".provider", file
        )

        val intent = Intent()
        intent.setDataAndType(uri, context.contentResolver.getType(uri))
        Log.d(GALLERY_SCREEN, "URI: ${intent.data}")
        Log.d(GALLERY_SCREEN, "MIME: ${intent.type}")

        intent.action = actionType
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val actionString = when (actionType) {
            Intent.ACTION_VIEW -> "View the resource with:"
            Intent.ACTION_EDIT -> {
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                "Edit the resource with:"
            }
            Intent.ACTION_SEND -> {
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                "Share the resource with:"
            }
            else -> "Open the resource with:"
        }
        if (detachProcess) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(intent, actionString)
        context.startActivity(chooser)
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
                chipBackgroundColor = requireActivity().getColorStateList(R.color.colorPrimary)
                chipStartPadding = getPXFromDP(12f)
                chipEndPadding = getPXFromDP(12f)
                textStartPadding = 0f
                textEndPadding = 0f

                setOnClickListener {
                    val position = binding.viewPager.currentItem
                    Log.d(GALLERY_SCREEN, "[edit_tags] clicked at position $position")
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

        fun newInstance(rootAndFav: RootAndFav, resources: List<ResourceId>, startAt: Int) =
            GalleryFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ROOT_AND_FAV_KEY, rootAndFav)
                    putLongArray(RESOURCES_KEY, resources.toLongArray())
                    putInt(START_AT_KEY, startAt)
                }
            }
    }
}