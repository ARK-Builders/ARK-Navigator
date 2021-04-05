package space.taran.arkbrowser.ui.fragments

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.dialog_sort.view.*
import space.taran.arkbrowser.R
import space.taran.arkbrowser.mvp.model.entity.File
import space.taran.arkbrowser.mvp.model.entity.Root
import space.taran.arkbrowser.mvp.model.entity.common.TagState
import space.taran.arkbrowser.mvp.presenter.TagsPresenter
import space.taran.arkbrowser.mvp.view.TagsView
import space.taran.arkbrowser.ui.App
import space.taran.arkbrowser.ui.MainActivity
import space.taran.arkbrowser.ui.adapter.FileGridRVAdapter
import kotlinx.android.synthetic.main.fragment_tags.*
import moxy.MvpAppCompatFragment
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import space.taran.arkbrowser.utils.SortBy


class TagsFragment(val root: Root? = null, val files: List<File>? = null, val state: TagsPresenter.State? = null) : MvpAppCompatFragment(), TagsView {

    @InjectPresenter
    lateinit var presenter: TagsPresenter

    @ProvidePresenter
    fun providePresenter() =
        TagsPresenter(root, files, state!!).apply {
            App.instance.appComponent.inject(this)
        }

    var adapter: FileGridRVAdapter? = null
    var sortByDialogView: View? = null
    var sortByDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_tags, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        App.instance.appComponent.inject(this)
    }

    override fun init() {
        (activity as MainActivity).setToolbarVisibility(true)
        rv_tags.layoutManager = GridLayoutManager(context, 3)
        adapter = FileGridRVAdapter(presenter.fileGridPresenter)
        rv_tags.adapter = adapter
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        presenter.onViewResumed()
    }

    override fun openFile(uri: String, mimeType: String) {
        val intent = Intent(Intent.ACTION_EDIT)
        val fileUri: Uri = Uri.parse(uri)
        intent.setDataAndType(fileUri, mimeType)
        startActivity(intent)
    }

    override fun updateAdapter() {
        adapter?.notifyDataSetChanged()
    }

    override fun setToolbarTitle(title: String) {
        (activity as MainActivity).setTitle(title)
    }

    override fun showSortByDialog(sortBy: SortBy, isReversedSort: Boolean) {
        sortByDialogView = LayoutInflater.from(context!!).inflate(R.layout.dialog_sort, null)
        val alertDialogBuilder = AlertDialog.Builder(context!!).setView(sortByDialogView)
        when (sortBy) {
            SortBy.NAME -> sortByDialogView!!.rb_name.isChecked = true
            SortBy.SIZE -> sortByDialogView!!.rb_size.isChecked = true
            SortBy.LAST_MODIFIED -> sortByDialogView!!.rb_last_modified.isChecked = true
            SortBy.TYPE -> sortByDialogView!!.rb_extension.isChecked = true
        }
        if (isReversedSort)
            sortByDialogView!!.rb_descending.isChecked = true
        else
            sortByDialogView!!.rb_ascending.isChecked = true

        sortByDialogView!!.rg_sort_by.setOnCheckedChangeListener { _, checkedId ->
            when(checkedId) {
                R.id.rb_name -> presenter.sortByChanged(SortBy.NAME)
                R.id.rb_size -> presenter.sortByChanged(SortBy.SIZE)
                R.id.rb_last_modified -> presenter.sortByChanged(SortBy.LAST_MODIFIED)
                R.id.rb_extension -> presenter.sortByChanged(SortBy.TYPE)
            }
        }

        sortByDialogView!!.rg_sort_reversed.setOnCheckedChangeListener { _, checkedId ->
            when(checkedId) {
                R.id.rb_ascending -> presenter.reversedSortChanged(false)
                R.id.rb_descending -> presenter.reversedSortChanged(true)
            }

        }

        sortByDialog = alertDialogBuilder.show()
        sortByDialog!!.setOnDismissListener {
            presenter.dismissDialog()
        }
    }

    override fun closeSortByDialog() {
        sortByDialog?.dismiss()
    }


    override fun setTags(tags: List<TagState>) {
        tags.forEach { tagState ->
            val chip = Chip(context)
            chip.isCheckable = true
            chip.text = tagState.tag
            chip.isChecked = tagState.isChecked
            if (tagState.isActual) {
                chip.chipBackgroundColor =
                    ColorStateList.valueOf(ContextCompat.getColor(context!!, R.color.colorPrimary))
                chip.setTextColor(ContextCompat.getColor(context!!, R.color.white))
            }
            chip.setOnClickListener {
                presenter.tagChecked(tagState.tag, chip.isChecked)
            }
            chipg_tags.addView(chip)
        }

        if (tags.size > 1) {
            val chip = Chip(context)
            chip.chipIcon = ContextCompat.getDrawable(context!!, R.drawable.ic_close)
            chip.chipIconTint = ColorStateList.valueOf(ContextCompat.getColor(context!!, R.color.black))
            chip.textStartPadding = 0f
            chip.textEndPadding = 0f
            chip.setOnClickListener {
                presenter.clearTagsChecked()
            }
            chipg_tags.addView(chip)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menu_tags_sort_by -> presenter.sortByMenuClicked()
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_tags_screen, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun clearTags() {
        chipg_tags.removeAllViews()
    }
}