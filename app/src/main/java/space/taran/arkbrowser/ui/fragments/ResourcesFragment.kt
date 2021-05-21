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
import kotlinx.android.synthetic.main.fragment_tags.*
import moxy.MvpAppCompatFragment
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import space.taran.arkbrowser.mvp.presenter.ResourcesGrid
import space.taran.arkbrowser.ui.adapter.ResourcesGridAdapter
import space.taran.arkbrowser.ui.fragments.utils.Notifications
import space.taran.arkbrowser.utils.RESOURCES_SCREEN
import space.taran.arkbrowser.utils.extension
import java.nio.file.Files
import java.nio.file.Path

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

    private lateinit var gridAdapter: ResourcesGridAdapter

    private var sorting: Sorting = Sorting.DEFAULT
    private var ascending: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        Log.d(RESOURCES_SCREEN, "creating view in ResourcesFragment")
        return inflater.inflate(R.layout.fragment_tags, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(RESOURCES_SCREEN, "view created in ResourcesFragment")
        super.onViewCreated(view, savedInstanceState)
        App.instance.appComponent.inject(this)
    }

    override fun init(grid: ResourcesGrid) {
        Log.d(RESOURCES_SCREEN, "initializing ResourcesFragment")
        (activity as MainActivity).setSelectedTab(1)
        (activity as MainActivity).setToolbarVisibility(true)
        setHasOptionsMenu(true)

        gridAdapter = ResourcesGridAdapter(grid)

        rv_tags.adapter = gridAdapter
        rv_tags.layoutManager = GridLayoutManager(context, 3)
    }

    override fun onResume() {
        Log.d(RESOURCES_SCREEN, "resuming in ResourcesFragment")
        super.onResume()
        presenter.onViewResumed()
    }

    override fun openFile(uri: Uri, mimeType: String) {
        Log.d(RESOURCES_SCREEN, "opening file $uri in ResourcesFragment")
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
        Log.d(RESOURCES_SCREEN, "updating adapter in ResourcesFragment")
        gridAdapter?.notifyDataSetChanged()
    }

    override fun setToolbarTitle(title: String) {
        (activity as MainActivity).setTitle(title)
    }

    override fun setTagsLayoutVisibility(isVisible: Boolean) {
        Log.d(RESOURCES_SCREEN, "setting tags layout visibility to $isVisible")
        chipg_tags.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    private fun showSortByDialog() {
        Log.d(RESOURCES_SCREEN, "showing sort-by dialog in ResourcesFragment")
        val view = LayoutInflater.from(context!!).inflate(R.layout.dialog_sort, null)!!
        val alertBuilder = AlertDialog.Builder(context!!).setView(view)

        when(sorting) {
            Sorting.DEFAULT -> view.rb_default.isChecked = true
            Sorting.NAME -> view.rb_name.isChecked = true
            Sorting.SIZE -> view.rb_size.isChecked = true
            Sorting.LAST_MODIFIED -> view.rb_last_modified.isChecked = true
            Sorting.TYPE -> view.rb_type.isChecked = true
        }

        if (sorting == Sorting.DEFAULT) {
            view.rb_ascending.isEnabled = false
            view.rb_descending.isEnabled = false
            view.rg_sorting_direction.isEnabled = false
        } else {
            if (ascending) {
                view.rb_ascending.isChecked = true
            } else {
                view.rb_descending.isChecked = true
            }
        }

        var dialog: AlertDialog? = null

        view.rg_sorting.setOnCheckedChangeListener { _, checkedId ->
            when(checkedId) {
                R.id.rb_default -> throw AssertionError("As-is sorting is initial, unsorted order")

                R.id.rb_name -> sorting = Sorting.NAME
                R.id.rb_size -> sorting = Sorting.SIZE
                R.id.rb_last_modified -> sorting = Sorting.LAST_MODIFIED
                R.id.rb_type -> sorting = Sorting.TYPE
            }
            Log.d(RESOURCES_SCREEN, "sorting criteria changed, sorting = $sorting")

            when(sorting) {
                Sorting.NAME -> gridAdapter.sortBy { it.fileName }
                Sorting.SIZE -> gridAdapter.sortBy { Files.size(it) }
                Sorting.TYPE -> gridAdapter.sortBy { extension(it) }
                Sorting.LAST_MODIFIED -> gridAdapter.sortBy { Files.getLastModifiedTime(it) }
                Sorting.DEFAULT -> throw AssertionError("Not possible")
            }

            ascending = true
            dialog!!.dismiss()
        }

        view.rg_sorting_direction.setOnCheckedChangeListener { _, checkedId ->
            when(checkedId) {
                R.id.rb_ascending -> ascending = true
                R.id.rb_descending -> ascending = false
            }
            Log.d(RESOURCES_SCREEN, "sorting direction changed, ascending = $ascending")

            gridAdapter.reverse()

            dialog!!.dismiss()
        }

        dialog = alertBuilder.show()
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
        Log.d(RESOURCES_SCREEN, "options item selected in ResourcesFragment")
        when(item.itemId) {
            R.id.menu_tags_sort_by -> showSortByDialog()
            R.id.menu_tags_tags_off -> presenter.tagsOffChanged()
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        Log.d(RESOURCES_SCREEN, "creating options menu in ResourcesFragment")
        inflater.inflate(R.menu.menu_tags_screen, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun clearTags() {
        Log.d(RESOURCES_SCREEN, "clearing tags in ResourcesFragment")
        chipg_tags.removeAllViews()
    }

    override fun notifyUser(message: String, moreTime: Boolean) {
        Notifications.notifyUser(context, message, moreTime)
    }
}

enum class Sorting {
    DEFAULT, NAME, SIZE, LAST_MODIFIED, TYPE
}