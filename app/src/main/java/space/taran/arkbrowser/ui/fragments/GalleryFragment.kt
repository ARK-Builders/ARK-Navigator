package space.taran.arkbrowser.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.chip.Chip
import space.taran.arkbrowser.R
import space.taran.arkbrowser.mvp.presenter.GalleryPresenter
import space.taran.arkbrowser.mvp.view.GalleryView
import space.taran.arkbrowser.ui.App
import space.taran.arkbrowser.ui.activity.MainActivity
import space.taran.arkbrowser.ui.adapter.PreviewsPager
import kotlinx.android.synthetic.main.dialog_tags.view.*
import kotlinx.android.synthetic.main.fragment_detail.*
import moxy.MvpAppCompatFragment
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import space.taran.arkbrowser.mvp.model.dao.ResourceId
import space.taran.arkbrowser.mvp.model.repo.ResourcesIndex
import space.taran.arkbrowser.mvp.model.repo.TagsStorage
import space.taran.arkbrowser.mvp.presenter.adapter.PreviewsList
import space.taran.arkbrowser.utils.*

//todo: use Bundle if resume doesn't work

class GalleryFragment(
    private val index: ResourcesIndex,
    private val storage: TagsStorage,
    private val resources: List<ResourceId>,
    private val startAt: Int)
    : MvpAppCompatFragment(), GalleryView, BackButtonListener {

    private var dialogView: View? = null
    private var dialog: AlertDialog? = null

    @InjectPresenter
    lateinit var presenter: GalleryPresenter

    @ProvidePresenter
    fun providePresenter() =
        GalleryPresenter(index, storage, resources, startAt).apply {
            Log.d(GALLERY_SCREEN, "creating GalleryPresenter")
            App.instance.appComponent.inject(this)
        }

    var adapter: PreviewsPager? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        Log.d(GALLERY_SCREEN, "inflating layout for GalleryFragment")
        return inflater.inflate(R.layout.fragment_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(GALLERY_SCREEN, "view created in GalleryFragment")
        super.onViewCreated(view, savedInstanceState)
        App.instance.appComponent.inject(this)
    }

    //        view_pager.adapter?.notifyDataSetChanged()

    override fun init(previews: PreviewsList) {
        Log.d(GALLERY_SCREEN, "initializing GalleryFragment, " +
            "position = $startAt")

        (activity as MainActivity).setToolbarVisibility(true)

        adapter = PreviewsPager(previews)

        view_pager.adapter = adapter
        view_pager.offscreenPageLimit = 2

        view_pager.setCurrentItem(previews.position(), false)

        view_pager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                Log.d(GALLERY_SCREEN, "[mock] changing to preview at position $position")
                val resource = resources[position]
                presenter
                setImageTags(storage.listTags(resource))
                setTitle(index.getPath(resource)!!.fileName.toString())
            }
        })

        edit_tags_fab.setOnClickListener {
            Log.d(GALLERY_SCREEN, "[mock] [edit_tags] clicked")
            showTagsDialog(storage.listTags(resources[previews.position()]))
        }
    }

    override fun setTitle(title: String) {
        (activity as MainActivity).setTitle(title)
    }

    override fun onPause() {
        Log.d(GALLERY_SCREEN, "pausing GalleryFragment")
        dialog?.dismiss()
        super.onPause()
    }

    override fun backClicked(): Boolean {
        Log.d(GALLERY_SCREEN, "[back] clicked in GalleryFragment")
        return presenter.backClicked()
    }

    private fun showTagsDialog(tags: Tags) {
        Log.d(GALLERY_SCREEN, "showing tags dialog in GalleryFragment")

        dialogView = LayoutInflater.from(context!!).inflate(R.layout.dialog_tags, null)
        val alertDialogBuilder = AlertDialog.Builder(context!!).setView(dialogView)

        if (tags.isNotEmpty()) {
            dialogView!!.chipg_dialog_detail.visibility = View.VISIBLE
        } else {
            dialogView!!.chipg_dialog_detail.visibility = View.GONE
        }

        //dialogView?.chipg_dialog_detail?.removeAllViews()

        tags.forEach { tag ->
            val chip = Chip(context)
            chip.text = tag
            chip.isCloseIconVisible = true
            chip.setOnCloseIconClickListener {
                removeTag(tag)
            }
            dialogView?.chipg_dialog_detail?.addView(chip)
        }

        dialogView!!.et_tags.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val newTags = Converters.tagsFromString(dialogView!!.et_tags.text.toString())
                if (newTags.isEmpty() || newTags.contains(Constants.EMPTY_TAG)) {
                    return@setOnEditorActionListener false
                }

                replaceTags(newTags)
                closeDialog()
                true
            } else {
                false
            }
        }

        dialog = alertDialogBuilder.show()
        dialog!!.setOnCancelListener {
            closeDialog()
        }
    }

    private fun closeDialog() {
        Log.d(GALLERY_SCREEN, "closing dialog in GalleryFragment")
        dialog?.dismiss()
    }

    private fun setDialogTags(imageTags: Tags) {
        Log.d(GALLERY_SCREEN, "setting dialog tags in GalleryFragment")

        if (imageTags.isNotEmpty())
            dialogView?.chipg_dialog_detail?.visibility = View.VISIBLE
        else
            dialogView?.chipg_dialog_detail?.visibility = View.GONE
        dialogView?.chipg_dialog_detail?.removeAllViews()
        imageTags.forEach { tag ->
            val chip = Chip(context)
            chip.text = tag
            chip.isCloseIconVisible = true
            chip.setOnCloseIconClickListener {
                removeTag(tag)
            }
            dialogView?.chipg_dialog_detail?.addView(chip)
        }

    }

    private fun setImageTags(imageTags: Tags) {
        Log.d(GALLERY_SCREEN, "setting image tags in GalleryFragment")

        if (imageTags.isEmpty())
            edit_tags_fab.visibility = View.VISIBLE
        else
            edit_tags_fab.visibility = View.GONE

        tags_cg.removeAllViews()

        imageTags.forEach { tag ->
            val chip = Chip(context)
            chip.text = tag
            chip.setOnClickListener {
                Log.d(GALLERY_SCREEN, "[mock] chip group clicked")
                //viewState.showTagsDialog(currentResource.tags)
            }
            tags_cg.addView(chip)
        }
    }

    private fun replaceTags(tags: Tags) {
        presenter.replaceTags(tags)
        setImageTags(tags)
        setDialogTags(tags)
    }

    private fun removeTag(tag: Tag) {
        Log.d(GALLERY_SCREEN, "[mock] tag $tag removed")
//        currentResource.tags = currentResource.tags - tag
//
//        setImageTags(currentResource.tags)
//        setDialogTags(currentResource.tags)
        presenter.removeTag(tag)
    }
}