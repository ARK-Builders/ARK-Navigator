package space.taran.arkbrowser.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.dialog_sort.view.*
import space.taran.arkbrowser.R
import space.taran.arkbrowser.mvp.presenter.ResourcesPresenter
import space.taran.arkbrowser.mvp.view.ResourcesView
import space.taran.arkbrowser.ui.App
import space.taran.arkbrowser.ui.activity.MainActivity
import kotlinx.android.synthetic.main.fragment_resources.*
import moxy.MvpAppCompatFragment
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import space.taran.arkbrowser.mvp.presenter.adapter.ResourcesList
import space.taran.arkbrowser.ui.adapter.ResourcesGrid
import space.taran.arkbrowser.ui.fragments.utils.Notifications
import space.taran.arkbrowser.mvp.presenter.TagsSelector
import space.taran.arkbrowser.utils.*
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

    private lateinit var gridAdapter: ResourcesGrid

    private lateinit var menuTagsOn: MenuItem
    private lateinit var menuTagsOff: MenuItem
    private var tagsEnabled = true

    private var sorting: Sorting = Sorting.DEFAULT
    private var ascending: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        Log.d(RESOURCES_SCREEN, "inflating layout for ResourcesFragment")
        return inflater.inflate(R.layout.fragment_resources, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(RESOURCES_SCREEN, "view created in ResourcesFragment")
        super.onViewCreated(view, savedInstanceState)

        App.instance.appComponent.inject(this)
    }

    //todo:
    //      case: open folder, click [tags off], then [tags on], open resource, go [back]
    //      expected: all resources displayed
    //      actual: only untagged resources displayed
    //      (apparently, wrong state restored)

    override fun init(grid: ResourcesList) {
        Log.d(RESOURCES_SCREEN, "initializing ResourcesFragment")
        (activity as MainActivity).setSelectedTab(1)
        (activity as MainActivity).setToolbarVisibility(true)
        setHasOptionsMenu(true)

        initResources(grid)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(RESOURCES_SCREEN, "options item selected in ResourcesFragment")
        when(item.itemId) {
            R.id.menu_tags_sort_by -> showSortByDialog()
            R.id.menu_tags_off -> disableTags()

            R.id.menu_tags_on -> {
                val tags = presenter.listTagsForAllResources()
                if (tags.isNotEmpty()) {
                    initResources(presenter.provideResources())
                } else {
                    notifyUser("Tag something first")
                }
            }
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        Log.d(RESOURCES_SCREEN, "inflating options menu in ResourcesFragment")
        inflater.inflate(R.menu.menu_tags_screen, menu)

        menuTagsOn = menu.findItem(R.id.menu_tags_on)
        menuTagsOff = menu.findItem(R.id.menu_tags_off)

        showTagsOnOffButtons()

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onResume() {
        Log.d(RESOURCES_SCREEN, "resuming in ResourcesFragment")
        super.onResume()
    }

    override fun setToolbarTitle(title: String) {
        (activity as MainActivity).setTitle(title)
    }

    override fun notifyUser(message: String, moreTime: Boolean) {
        Notifications.notifyUser(context, message, moreTime)
    }

    private fun initResources(resources: ResourcesList) {
        gridAdapter = ResourcesGrid(resources)

        rv_resources.adapter = gridAdapter
        rv_resources.layoutManager = GridLayoutManager(context, 3)

        val selector = presenter.createTagsSelector()
        if (selector != null) {
            enableTags(selector)
        } else {
            notifyUser("You don't have any tags here yet", moreTime = true)
            disableTags()
        }
    }

    //showing all resources, displaying tags selector
    private fun enableTags(selector: TagsSelector) {
        Log.d(RESOURCES_SCREEN, "enabling tags mode")

        tagsEnabled = true
        showTagsOnOffButtons()

        //todo: selector produces `Query` which is only a set of tags right now
        //(so it can represent only conjunction of tags), but will change in future
        //in the way it will also support negation and disjunction
        tags_cg.visibility = View.VISIBLE

        selector.draw(tags_cg, context!!) { selection ->
            gridAdapter.updateItems(selection.toList())
        }
    }

    //showing only untagged resources, hiding tags selector
    private fun disableTags() {
        Log.d(RESOURCES_SCREEN, "disabling tags mode")

        tagsEnabled = false
        showTagsOnOffButtons()

        tags_cg.visibility = View.GONE

        val untagged = presenter.listUntaggedResources()
        gridAdapter.updateItems(untagged.toList())
    }

    private fun showTagsOnOffButtons() {
        if (this::menuTagsOn.isInitialized && this::menuTagsOff.isInitialized) {
            Log.d(RESOURCES_SCREEN, "showing tags selector? $tagsEnabled")
            menuTagsOn.isVisible = !tagsEnabled
            menuTagsOff.isVisible = tagsEnabled
        }
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
}