package space.taran.arkbrowser.ui.fragments

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.dialog_sort.view.*
import space.taran.arkbrowser.R
import space.taran.arkbrowser.mvp.model.entity.common.TagState
import space.taran.arkbrowser.mvp.presenter.ResourcesPresenter
import space.taran.arkbrowser.mvp.view.ResourcesView
import space.taran.arkbrowser.ui.App
import space.taran.arkbrowser.ui.activity.MainActivity
import space.taran.arkbrowser.ui.adapter.ItemGridRVAdapter
import kotlinx.android.synthetic.main.fragment_tags.*
import moxy.InjectViewState
import moxy.MvpAppCompatFragment
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import space.taran.arkbrowser.mvp.model.entity.room.ResourceId
import space.taran.arkbrowser.mvp.model.repo.FoldersRepo
import space.taran.arkbrowser.mvp.model.repo.TagsStorage
import space.taran.arkbrowser.ui.fragments.utils.Notifications
import space.taran.arkbrowser.utils.SortBy
import space.taran.arkbrowser.utils.RESOURCES_SCREEN
import java.nio.file.Path
import javax.inject.Inject

//`root` is used for querying tags storage and resources index,
//       if it is `null`, then resources from all roots are taken
//                        and tags storage for every particular resource
//                        is determined dynamically
//
//`path` is used for filtering resources' paths
//       if it is `null`, then no filtering is performed
//       (recommended instead of passing same value for `path` and `root)
class ResourcesFragment(val root: Path?, val path: Path?) : MvpAppCompatFragment(), ResourcesView {

    @InjectPresenter
    lateinit var presenter: ResourcesPresenter

    @ProvidePresenter
    fun providePresenter() =
        ResourcesPresenter(root, path).apply {
            Log.d(RESOURCES_SCREEN, "creating ResourcesPresenter")
            App.instance.appComponent.inject(this)
        }

    private var adapter: ItemGridRVAdapter<Unit, ResourceId>? = null
    private var sortByDialogView: View? = null
    private var sortByDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(RESOURCES_SCREEN, "creating view in TagsFragment")
        return inflater.inflate(R.layout.fragment_tags, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(RESOURCES_SCREEN, "view created in TagsFragment")
        super.onViewCreated(view, savedInstanceState)
        App.instance.appComponent.inject(this)
    }

    override fun init() {
        Log.d(RESOURCES_SCREEN, "initializing TagsFragment")
        (activity as MainActivity).setSelectedTab(1)
        (activity as MainActivity).setToolbarVisibility(true)
        rv_tags.layoutManager = GridLayoutManager(context, 3)
        adapter = ItemGridRVAdapter(presenter.fileGridPresenter) //todo
        rv_tags.adapter = adapter
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        Log.d(RESOURCES_SCREEN, "resuming in TagsFragment")
        super.onResume()
        presenter.onViewResumed()
    }

    override fun openFile(uri: Uri, mimeType: String) {
        Log.d(RESOURCES_SCREEN, "opening file $uri in TagsFragment")
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, mimeType)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No app can handle this file", Toast.LENGTH_SHORT).show()
        }
    }

    override fun updateAdapter() {
        Log.d(RESOURCES_SCREEN, "updating adapter in TagsFragment")
        adapter?.notifyDataSetChanged()
    }

    override fun setToolbarTitle(title: String) {
        (activity as MainActivity).setTitle(title)
    }

    override fun setTagsLayoutVisibility(isVisible: Boolean) {
        Log.d(RESOURCES_SCREEN, "setting tags layout visibility to $isVisible")
        chipg_tags.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    override fun showSortByDialog(sortBy: SortBy, isReversedSort: Boolean) {
        Log.d(RESOURCES_SCREEN, "showing sort-by dialog ($sortBy, $isReversedSort) in TagsFragment")
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
        Log.d(RESOURCES_SCREEN, "closing sort-by dialog in TagsFragment")
        sortByDialog?.dismiss()
    }


    override fun setTags(tags: List<TagState>) {
        Log.d(RESOURCES_SCREEN, "setting tags states to $tags")
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
        Log.d(RESOURCES_SCREEN, "options item selected in TagsFragment")
        when(item.itemId) {
            R.id.menu_tags_sort_by -> presenter.sortByMenuClicked()
            R.id.menu_tags_tags_off -> presenter.tagsOffChanged()
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        Log.d(RESOURCES_SCREEN, "creating options menu in TagsFragment")
        inflater.inflate(R.menu.menu_tags_screen, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun clearTags() {
        Log.d(RESOURCES_SCREEN, "clearing tags in TagsFragment")
        chipg_tags.removeAllViews()
    }

    override fun notifyUser(message: String, moreTime: Boolean) {
        Notifications.notifyUser(context, message, moreTime)
    }
}

//if (path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg")) {
//    view.setIcon(IconOrImage(image = path))
//} else {
//    view.setIcon(IconOrImage(icon = Icon.FILE))
//}