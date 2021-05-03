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
import space.taran.arkbrowser.mvp.presenter.DetailPresenter
import space.taran.arkbrowser.mvp.view.DetailView
import space.taran.arkbrowser.ui.App
import space.taran.arkbrowser.ui.activity.MainActivity
import space.taran.arkbrowser.ui.adapter.DetailVPAdapter
import kotlinx.android.synthetic.main.dialog_tags.view.*
import kotlinx.android.synthetic.main.fragment_detail.*
import moxy.MvpAppCompatFragment
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import space.taran.arkbrowser.mvp.model.entity.room.ResourceId
import space.taran.arkbrowser.utils.Tags

class DetailFragment: MvpAppCompatFragment(), DetailView, BackButtonListener {
    companion object {
        const val RESOURCES_KEY = "resources"
        const val POSITION_KEY = "pos"

        fun newInstance(resources: List<ResourceId>, pos: Int) = DetailFragment().apply {
            Log.d("flow", "creating DetailFragment")
            arguments = Bundle().apply {
                putInt(POSITION_KEY, pos)
                putLongArray(RESOURCES_KEY, resources.toLongArray())
            }
        }
    }

    private var dialogView: View? = null
    private var dialog: AlertDialog? = null

    @InjectPresenter
    lateinit var presenter: DetailPresenter

    @ProvidePresenter
    fun providePresenter(): DetailPresenter {
        Log.d("flow", "creating DetailPresenter")

        return DetailPresenter(
            arguments!!.getParcelableArray(RESOURCES_KEY)!!.map { it as ResourceId },
            arguments!!.getInt(POSITION_KEY)
        ).apply {
            App.instance.appComponent.inject(this)
        }
    }

    var adapter: DetailVPAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("flow", "creating view in DetailFragment")
        return inflater.inflate(R.layout.fragment_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("flow", "view created in DetailFragment")
        super.onViewCreated(view, savedInstanceState)
        App.instance.appComponent.inject(this)
    }

    override fun init() {
        Log.d("flow", "initializing DetailFragment")

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
        Log.d("flow", "setting current item to $pos in DetailFragment")
        view_pager.setCurrentItem(pos, false)
    }

    override fun updateAdapter() {
        Log.d("flow", "updating adapter in DetailFragment")
        view_pager.adapter?.notifyDataSetChanged()
    }

    override fun showTagsDialog(imageTags: Tags) {
        Log.d("flow", "showing tags dialog in DetailFragment")

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
        Log.d("flow", "closing dialog in DetailFragment")
        dialog?.dismiss()
    }

    override fun setDialogTags(imageTags: Tags) {
        Log.d("flow", "setting dialog tags in DetailFragment")

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
        Log.d("flow", "setting image tags in DetailFragment")

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
        Log.d("flow", "pausing DetailFragment")
        dialog?.dismiss()
        super.onPause()
    }

    override fun backClicked(): Boolean {
        Log.d("flow", "back clicked in DetailFragment")
        return presenter.backClicked()
    }

}