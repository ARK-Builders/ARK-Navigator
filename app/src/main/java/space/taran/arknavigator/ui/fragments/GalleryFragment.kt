package space.taran.arknavigator.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.EditorInfo
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.chip.Chip
import moxy.MvpAppCompatFragment
import moxy.ktx.moxyPresenter
import space.taran.arknavigator.BuildConfig
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.DialogTagsBinding
import space.taran.arknavigator.databinding.FragmentGalleryBinding
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.repo.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.TagsStorage
import space.taran.arknavigator.mvp.presenter.GalleryPresenter
import space.taran.arknavigator.mvp.presenter.adapter.PreviewsList
import space.taran.arknavigator.mvp.view.GalleryView
import space.taran.arknavigator.mvp.view.NotifiableView
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.ui.activity.MainActivity
import space.taran.arknavigator.ui.adapter.PreviewsPager
import space.taran.arknavigator.ui.fragments.utils.Notifications
import space.taran.arknavigator.utils.*
import space.taran.arknavigator.utils.extensions.makeGone
import space.taran.arknavigator.utils.extensions.makeVisibleAndSetOnClickListener
import space.taran.arknavigator.ui.fragments.utils.Preview.ExtraInfoTag.*
import java.io.File

class GalleryFragment(
    private val index: ResourcesIndex,
    private val storage: TagsStorage,
    private val resources: MutableList<ResourceId>,
    private val startAt: Int
) : MvpAppCompatFragment(), GalleryView, BackButtonListener, NotifiableView {

    private var dialogBinding: DialogTagsBinding? = null
    private var dialog: AlertDialog? = null
    private lateinit var binding: FragmentGalleryBinding

    private val presenter by moxyPresenter {
        GalleryPresenter(index, storage, resources).apply {
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

    override fun init(previews: PreviewsList) {
        Log.d(GALLERY_SCREEN, "initializing GalleryFragment, position = $startAt")
        Log.d(GALLERY_SCREEN, "currentItem = ${binding.viewPager.currentItem}")

        (activity as MainActivity).setToolbarVisibility(true)

        requireActivity().window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                presenter.onSystemUIVisibilityChange(true)
            } else {
                presenter.onSystemUIVisibilityChange(false)
            }
        }

        pagerAdapter = PreviewsPager(previews)

        binding.apply {
            viewPager.apply {
                adapter = pagerAdapter
                offscreenPageLimit = 2
                setCurrentItem(startAt, false)

                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    private var workaround = true

                    override fun onPageSelected(position: Int) {
                        if (this@GalleryFragment.resources.isEmpty()) {
                            return
                        }

                        if (startAt > 0 || !workaround) {
                            //weird bug causes this callback be called redundantly if startAt == 0
                            Log.d(GALLERY_SCREEN, "changing to preview at position $position")
                            setupOpenEditFABs()
                            displayPreview(position)
                        }
                        workaround = false
                    }
                })
            }

            displayPreview(startAt)

            removeResourceFab.setOnLongClickListener {
                val position = viewPager.currentItem
                Log.d(GALLERY_SCREEN, "[remove_resource] long-clicked at position $position")
                deleteResource(position)
                true
            }

            shareResourceFab.setOnClickListener {
                val position = viewPager.currentItem
                Log.d(GALLERY_SCREEN, "[share_resource] clicked at position $position")
                shareResource(position)
            }

            setupOpenEditFABs()
        }
    }

    override fun setPreviewsScrollingEnabled(enabled: Boolean) {
        binding.viewPager.isUserInputEnabled = enabled
    }

    override fun setFullscreen(fullscreen: Boolean) {
        val isControlsVisible = !fullscreen
        (activity as MainActivity).setBottomNavigationVisibility(isControlsVisible)
        (activity as MainActivity).setToolbarVisibility(isControlsVisible)
        binding.previewControls.isVisible = isControlsVisible
        FullscreenHelper.setSystemUIVisibility(isControlsVisible, requireActivity().window)
    }

    override fun setTitle(title: String) {
        activity?.title = title
    }

    override fun onPause() {
        Log.d(GALLERY_SCREEN, "pausing GalleryFragment")
        dialog?.dismiss()
        super.onPause()
    }

    override fun backClicked(): Boolean {
        Log.d(GALLERY_SCREEN, "[back] clicked in GalleryFragment")
        return presenter.quit()
    }

    override fun notifyUser(message: String, moreTime: Boolean) {
        Notifications.notifyUser(context, message, moreTime)
    }

    private fun deleteResource(position: Int) {
        pagerAdapter.removeItem(position)
        val resource = resources.removeAt(position)

        presenter.deleteResource(resource)

        if (resources.isEmpty()) {
            presenter.quit()
        }
    }

    private fun shareResource(position: Int) {
        val resource = resources[position]
        val path = index.getPath(resource)!!

        val context = requireContext()
        val uri = FileProvider.getUriForFile(
            context, "space.taran.arknavigator.provider",
            path.toFile()
        )
        val mime = context.contentResolver.getType(uri)
        Log.d(GALLERY_SCREEN, "sharing $uri of type $mime")

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = mime
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        startActivity(Intent.createChooser(intent, "Share using"))
    }

    private fun showEditTagsDialog(position: Int) {
        val resource = resources[position]
        Log.d(GALLERY_SCREEN, "showing [edit-tags] dialog for position $position")
        showEditTagsDialog(resource)
    }

    private fun setupOpenEditFABs() {

        val currentPosition = binding.viewPager.currentItem

        binding.apply {
            openResourceChooserFab.setOnClickListener(null)
            openFileEditFab.setOnClickListener(null)

            val resource = resources[currentPosition]
            val filePath = index.getPath(resource)

            when (getFileActionType(filePath!!)) {
                FileActionType.OPEN_ONLY -> {
                    openFileEditFab.makeGone()
                    openResourceChooserFab.makeVisibleAndSetOnClickListener{
                        openIntentChooser(currentPosition, Intent.ACTION_VIEW)
                    }
                }
                FileActionType.OPEN_ONLY_DETACH_PROCESS -> {
                    openFileEditFab.makeGone()
                    openResourceChooserFab.makeVisibleAndSetOnClickListener{
                        openIntentChooser(currentPosition, Intent.ACTION_VIEW, true)
                    }
                }
                FileActionType.EDIT_AND_OPEN -> {
                    openFileEditFab.makeVisibleAndSetOnClickListener {
                        openIntentChooser(currentPosition, Intent.ACTION_EDIT) }

                    openResourceChooserFab.makeVisibleAndSetOnClickListener {
                        openIntentChooser(currentPosition, Intent.ACTION_VIEW, true) }
                }

                FileActionType.EDIT_AS_OPEN -> {
                    openResourceChooserFab.makeGone()
                    openFileEditFab.makeVisibleAndSetOnClickListener {
                        openIntentChooser(currentPosition, Intent.ACTION_EDIT) }
                }
            }
        }
    }

    private fun openIntentChooser(position: Int, actionType: String, detachProcess: Boolean = false) {
        val resource = resources[position]
        val filePath = index.getPath(resource)

        //Create intent, and set the data and extension to it
        val intent = Intent()
        intent.action = actionType
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        if (detachProcess)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val file = File(filePath.toString())
        val extension = ".${extension(filePath!!)}"

        val fileURI = FileProvider.getUriForFile(
            requireContext(),
            BuildConfig.APPLICATION_ID + ".provider", file
        )

        intent.setDataAndType(
            fileURI,
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        )

        val actionString = if (actionType == Intent.ACTION_VIEW)
            "Open with:"
        else "Choose application to edit:"
        requireContext().startActivity(Intent.createChooser(intent, actionString))
    }

    private fun showEditTagsDialog(resource: ResourceId) {
        Log.d(GALLERY_SCREEN, "showing [edit-tags] dialog for resource $resource")

        val tags = presenter.listTags(resource)

        dialogBinding = DialogTagsBinding.inflate(LayoutInflater.from(requireContext()))

        val alertDialogBuilder = AlertDialog.Builder(requireContext()).setView(dialogBinding?.root)

        if (tags.isNotEmpty()) {
            dialogBinding?.chipgDialogDetail?.visibility = View.VISIBLE
        } else {
            dialogBinding?.chipgDialogDetail?.visibility = View.GONE
        }

        dialogBinding?.chipgDialogDetail?.removeAllViews()

        displayDialogTags(resource, tags)

        dialogBinding?.newTags?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val newTags = Converters.tagsFromString(dialogBinding?.newTags?.text.toString())
                if (newTags.isEmpty() || newTags.contains(Constants.EMPTY_TAG)) {
                    return@setOnEditorActionListener false
                }

                replaceTags(resource, tags + newTags)
                true
            } else {
                false
            }
        }

        dialog = alertDialogBuilder.show()
        dialog!!.setOnCancelListener {
            closeEditTagsDialog()
        }
    }

    private fun displayPreview(position: Int) {
        val resource = resources[position]
        val tags = presenter.listTags(resource)
        displayPreviewTags(resource, tags)
        setTitle(index.getPath(resource)!!.fileName.toString())

        val extraInfo = presenter.getExtraInfoAt(position)
        if (extraInfo != null){
            binding.apply {
                resolutionTV.text = extraInfo[MEDIA_RESOLUTION]
                durationTV.text = extraInfo[MEDIA_DURATION]

                resolutionTV.visibility = if (extraInfo[MEDIA_RESOLUTION] == null) View.GONE
                else View.VISIBLE

                durationTV.visibility = if (extraInfo[MEDIA_DURATION] == null) View.GONE
                else View.VISIBLE
            }
        } else {
            binding.durationTV.visibility = View.GONE
            binding.resolutionTV.visibility = View.GONE
        }
    }

    private fun displayDialogTags(resource: ResourceId, tags: Tags) {
        Log.d(GALLERY_SCREEN, "displaying tags resource $resource for edit")

        if (tags.isNotEmpty()) {
            dialogBinding?.chipgDialogDetail?.visibility = View.VISIBLE
        } else {
            dialogBinding?.chipgDialogDetail?.visibility = View.GONE
        }
        dialogBinding?.chipgDialogDetail?.removeAllViews()

        tags.forEach { tag ->
            val chip = Chip(context)
            chip.text = tag
            chip.isCloseIconVisible = true
            chip.setOnCloseIconClickListener {
                Log.d(GALLERY_SCREEN, "tag $tag on resource $resource close-icon-clicked")
                removeTag(resource, tags, tag)
            }
            dialogBinding?.chipgDialogDetail?.addView(chip)
        }
    }

    private fun displayPreviewTags(resource: ResourceId, tags: Tags) {
        Log.d(GALLERY_SCREEN, "displaying tags of resource $resource for preview")

        binding.tagsCg.removeAllViews()

        tags.forEach { tag ->
            val chip = Chip(context)
            chip.text = tag

            chip.setOnLongClickListener {
                Log.d(GALLERY_SCREEN, "tag $tag on resource $resource long-clicked")
                removeTag(resource, tags, tag)
                true
            }

            binding.tagsCg.addView(chip)
        }

        binding.tagsCg.addView(createEditChip())
    }

    private fun removeTag(resource: ResourceId, tags: Tags, tag: Tag) {
        notifyUser("Tag \"$tag\" removed")
        replaceTags(resource, tags - tag)
    }

    private fun replaceTags(resource: ResourceId, tags: Tags) {
        closeEditTagsDialog()
        presenter.replaceTags(resource, tags)
        displayPreviewTags(resource, tags)
        displayDialogTags(resource, tags)
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
                    showEditTagsDialog(position)
                }
            }
        }
    }

    private fun closeEditTagsDialog() {
        Log.d(GALLERY_SCREEN, "closing dialog in GalleryFragment")
        dialog?.dismiss()
    }
}