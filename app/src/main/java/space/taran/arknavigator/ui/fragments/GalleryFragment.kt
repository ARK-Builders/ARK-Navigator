package space.taran.arknavigator.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.dialog_tags.view.*
import kotlinx.android.synthetic.main.fragment_gallery.*
import moxy.MvpAppCompatFragment
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import space.taran.arknavigator.R
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.repo.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.TagsStorage
import space.taran.arknavigator.mvp.presenter.GalleryPresenter
import space.taran.arknavigator.mvp.presenter.adapter.PreviewsList
import space.taran.arknavigator.mvp.presenter.adapter.ResourcesList
import space.taran.arknavigator.mvp.view.GalleryView
import space.taran.arknavigator.mvp.view.NotifiableView
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.ui.activity.MainActivity
import space.taran.arknavigator.ui.adapter.PreviewsPager
import space.taran.arknavigator.ui.fragments.utils.Notifications
import space.taran.arknavigator.utils.*

//todo: use Bundle if resume doesn't work

class GalleryFragment(
    private val index: ResourcesIndex,
    private val storage: TagsStorage,
    private val resources: ResourcesList,
    private val startAt: Int
)
    : MvpAppCompatFragment(), GalleryView, BackButtonListener, NotifiableView {

//    private val resources = resources.toMutableList()

    private var dialogView: View? = null
    private var dialog: AlertDialog? = null

    @InjectPresenter
    lateinit var presenter: GalleryPresenter

    @ProvidePresenter
    fun providePresenter() =
        GalleryPresenter(index, storage, resources).apply {
            Log.d(GALLERY_SCREEN, "creating GalleryPresenter")
            App.instance.appComponent.inject(this)
        }

    private lateinit var pagerAdapter: PreviewsPager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Log.d(GALLERY_SCREEN, "inflating layout for GalleryFragment")
        return inflater.inflate(R.layout.fragment_gallery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(GALLERY_SCREEN, "view created in GalleryFragment")
        super.onViewCreated(view, savedInstanceState)
        App.instance.appComponent.inject(this)
    }

    override fun init(previews: PreviewsList) {
        Log.d(GALLERY_SCREEN, "initializing GalleryFragment, position = $startAt")
        Log.d(GALLERY_SCREEN, "currentItem = ${view_pager.currentItem}")

        (activity as MainActivity).setToolbarVisibility(true)

        requireActivity().window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                presenter.onSystemUIVisibilityChange(true)
            } else {
                presenter.onSystemUIVisibilityChange(false)
            }
        }

        pagerAdapter = PreviewsPager(previews)

        view_pager.adapter = pagerAdapter
        view_pager.offscreenPageLimit = 2

        view_pager.setCurrentItem(startAt, false)

        view_pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            private var workaround = true

            override fun onPageSelected(position: Int) {
                if (resources.items().isEmpty()) {
                    return
                }

                if (startAt > 0 || !workaround) {
                    //weird bug causes this callback be called redundantly if startAt == 0
                    Log.d(GALLERY_SCREEN, "changing to preview at position $position")
                    displayPreview(position)
                }
                workaround = false
            }
        })

        displayPreview(startAt)

        remove_resource_fab.setOnLongClickListener {
            val position = view_pager.currentItem
            Log.d(GALLERY_SCREEN, "[remove_resource] long-clicked at position $position")
            deleteResource(position)
            true
        }

        share_resource_fab.setOnClickListener {
            val position = view_pager.currentItem
            Log.d(GALLERY_SCREEN, "[share_resource] clicked at position $position")
            shareResource(position)
        }

        edit_tags_fab.setOnClickListener {
            val position = view_pager.currentItem
            Log.d(GALLERY_SCREEN, "[edit_tags] clicked at position $position")
            showEditTagsDialog(position)
        }
    }

    override fun setFullscreen(fullscreen: Boolean) {
        val isControlsVisible = !fullscreen
        (activity as MainActivity).setBottomNavigationVisibility(isControlsVisible)
        (activity as MainActivity).setToolbarVisibility(isControlsVisible)
        preview_controls.isVisible = isControlsVisible
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

        if (resources.items().isEmpty()) {
            presenter.quit()
        }
    }

    private fun shareResource(position: Int) {
        val resource = resources.items()[position]
        val path = index.getPath(resource)!!

        val context = requireContext()
        val uri = FileProvider.getUriForFile(
            context, "space.taran.arknavigator.provider",
            path.toFile())
        val mime = context.contentResolver.getType(uri)
        Log.d(GALLERY_SCREEN, "sharing $uri of type $mime")

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = mime
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        startActivity(Intent.createChooser(intent, "Share using"))
    }

    private fun showEditTagsDialog(position: Int) {
        val resource = resources.items()[position]
        Log.d(GALLERY_SCREEN, "showing [edit-tags] dialog for position $position")
        showEditTagsDialog(resource)
    }

    private fun showEditTagsDialog(resource: ResourceId) {
        Log.d(GALLERY_SCREEN, "showing [edit-tags] dialog for resource $resource")

        val tags = presenter.listTags(resource)

        dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_tags, null)
        val alertDialogBuilder = AlertDialog.Builder(requireContext()).setView(dialogView)

        if (tags.isNotEmpty()) {
            dialogView!!.chipg_dialog_detail.visibility = View.VISIBLE
        } else {
            dialogView!!.chipg_dialog_detail.visibility = View.GONE
        }

        dialogView?.chipg_dialog_detail?.removeAllViews()

        displayDialogTags(resource, tags)

        dialogView!!.new_tags.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val newTags = Converters.tagsFromString(dialogView!!.new_tags.text.toString())
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
        val resource = resources.items()[position]
        val tags = presenter.listTags(resource)
        displayPreviewTags(resource, tags)
        setTitle(index.getPath(resource)!!.fileName.toString())
    }

    private fun displayDialogTags(resource: ResourceId, tags: Tags) {
        Log.d(GALLERY_SCREEN, "displaying tags resource $resource for edit")

        if (tags.isNotEmpty()) {
            dialogView?.chipg_dialog_detail?.visibility = View.VISIBLE
        } else {
            dialogView?.chipg_dialog_detail?.visibility = View.GONE
        }
        dialogView?.chipg_dialog_detail?.removeAllViews()

        tags.forEach { tag ->
            val chip = Chip(context)
            chip.text = tag
            chip.isCloseIconVisible = true
            chip.setOnCloseIconClickListener {
                Log.d(GALLERY_SCREEN, "tag $tag on resource $resource close-icon-clicked")
                removeTag(resource, tags, tag)
            }
            dialogView?.chipg_dialog_detail?.addView(chip)
        }
    }

    private fun displayPreviewTags(resource: ResourceId, tags: Tags) {
        Log.d(GALLERY_SCREEN, "displaying tags of resource $resource for preview")

        tags_cg.removeAllViews()

        tags.forEach { tag ->
            val chip = Chip(context)
            chip.text = tag

            chip.setOnLongClickListener {
                Log.d(GALLERY_SCREEN, "tag $tag on resource $resource long-clicked")
                removeTag(resource, tags, tag)
                true
            }

            tags_cg.addView(chip)
        }
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

    private fun closeEditTagsDialog() {
        Log.d(GALLERY_SCREEN, "closing dialog in GalleryFragment")
        dialog?.dismiss()
    }
}