package space.taran.arkbrowser.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.chip.Chip
import space.taran.arkbrowser.R
import space.taran.arkbrowser.mvp.model.entity.File
import space.taran.arkbrowser.mvp.model.entity.Root
import space.taran.arkbrowser.mvp.presenter.DetailPresenter
import space.taran.arkbrowser.mvp.view.DetailView
import space.taran.arkbrowser.ui.App
import space.taran.arkbrowser.ui.MainActivity
import space.taran.arkbrowser.ui.adapter.DetailVPAdapter
import kotlinx.android.synthetic.main.dialog_tags.view.*
import kotlinx.android.synthetic.main.fragment_detail.*
import moxy.MvpAppCompatFragment
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import space.taran.arkbrowser.utils.Tags

class DetailFragment: MvpAppCompatFragment(), DetailView, BackButtonListener {
    companion object {
        const val ROOT_KEY = "root"
        const val IMAGES_KEY = "files"
        const val POS_KEY = "pos"

        fun newInstance(root: Root, files: List<File>, pos: Int) = DetailFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ROOT_KEY, root)
                putInt(POS_KEY, pos)
                putParcelableArray(IMAGES_KEY, files.toTypedArray())
            }
        }
    }

    var dialogView: View? = null
    var dialog: AlertDialog? = null

    @InjectPresenter
    lateinit var presenter: DetailPresenter

    @ProvidePresenter
    fun providePresenter() = DetailPresenter(
        arguments!!.getParcelable(ROOT_KEY)!!,
        arguments!!.getParcelableArray(IMAGES_KEY)!!.map { it as File },
        arguments!!.getInt(POS_KEY)
    ).apply {
        App.instance.appComponent.inject(this)
    }

    var adapter: DetailVPAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        App.instance.appComponent.inject(this)
    }

    override fun init() {
        (activity as MainActivity).setToolbarVisibility(true)
        adapter = DetailVPAdapter(presenter.detailListPresenter)
        view_pager.adapter = adapter
        view_pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                presenter.imageChanged(position)
            }
        })
        view_pager.offscreenPageLimit = 2
        fab_explorer_fav.setOnClickListener {
            presenter.fabClicked()
        }
    }

    override fun setTitle(title: String) {
        (activity as MainActivity).setTitle(title)
    }

    override fun setCurrentItem(pos: Int) {
        view_pager.setCurrentItem(pos, false)
    }

    override fun updateAdapter() {
        view_pager.adapter?.notifyDataSetChanged()
    }

    override fun showTagsDialog(imageTags: Tags) {
        dialogView = LayoutInflater.from(context!!).inflate(R.layout.dialog_tags, null)
        val alertDialogBuilder = AlertDialog.Builder(context!!).setView(dialogView)

        if (imageTags.isNotEmpty())
            dialogView!!.chipg_dialog_detail.visibility = View.VISIBLE
        else
            dialogView!!.chipg_dialog_detail.visibility = View.GONE

        dialogView?.chipg_dialog_detail?.removeAllViews()
        imageTags.forEach { tag ->
            val chip = Chip(context)
            chip.text = tag
            chip.isCloseIconVisible = true
            chip.setOnCloseIconClickListener {
                presenter.tagRemoved(tag)
            }
            dialogView?.chipg_dialog_detail?.addView(chip)
        }

        dialogView!!.et_tags.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val tags = dialogView!!.et_tags.text.toString()

                !presenter.tagsAdded(tags)
            } else {
                false
            }
        }

        dialog = alertDialogBuilder.show()
        dialog!!.setOnCancelListener {
            presenter.dismissDialog()
        }
    }

    override fun closeDialog() {
        dialog?.dismiss()
    }

    override fun setDialogTags(imageTags: Tags) {
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
                presenter.tagRemoved(tag)
            }
            dialogView?.chipg_dialog_detail?.addView(chip)
        }

    }

    override fun setImageTags(imageTags: Tags) {
        if (imageTags.isEmpty())
            fab_explorer_fav.visibility = View.VISIBLE
        else
            fab_explorer_fav.visibility = View.GONE
        chipg_detail.removeAllViews()
        imageTags.forEach { tag ->
            val chip = Chip(context)
            chip.text = tag
            chip.setOnClickListener {
                presenter.chipGroupClicked()
            }
            chipg_detail.addView(chip)
        }
    }

    override fun onPause() {
        dialog?.dismiss()
        super.onPause()
    }

    override fun backClicked() = presenter.backClicked()

}