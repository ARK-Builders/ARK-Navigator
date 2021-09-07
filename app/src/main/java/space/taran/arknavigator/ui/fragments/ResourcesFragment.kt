package space.taran.arknavigator.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.dialog_sort.view.*
import kotlinx.android.synthetic.main.fragment_resources.*
import kotlinx.android.synthetic.main.layout_progress.*
import moxy.MvpAppCompatFragment
import moxy.ktx.moxyPresenter
import space.taran.arknavigator.R
import space.taran.arknavigator.extensions.changeEnabledStatus
import space.taran.arknavigator.mvp.presenter.ResourcesPresenter
import space.taran.arknavigator.mvp.presenter.TagsSelector
import space.taran.arknavigator.mvp.presenter.adapter.ResourcesList
import space.taran.arknavigator.mvp.view.ResourcesView
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.ui.activity.MainActivity
import space.taran.arknavigator.ui.adapter.ResourcesGrid
import space.taran.arknavigator.ui.fragments.utils.Notifications
import space.taran.arknavigator.utils.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.abs


//`root` is used for querying tags storage and resources index,
//       if it is `null`, then resources from all roots are taken
//                        and tags storage for every particular resource
//                        is determined dynamically
//
//`path` is used for filtering resources' paths
//       if it is `null`, then no filtering is performed
//       (recommended instead of passing same value for `path` and `root)
class ResourcesFragment(val root: Path?, val path: Path?): MvpAppCompatFragment(), ResourcesView {

    private val presenter by moxyPresenter {
        ResourcesPresenter(root, path).apply {
            Log.d(RESOURCES_SCREEN, "creating ResourcesPresenter")
            App.instance.appComponent.inject(this)
        }
    }

    private lateinit var gridAdapter: ResourcesGrid
    private var tagsSelector: TagsSelector? = null

    private lateinit var menuTagsOn: MenuItem
    private lateinit var menuTagsOff: MenuItem
    private var tagsEnabled = true

    private val frameTop by lazy {
        val loc = IntArray(2)
        layout_root.getLocationOnScreen(loc)
        loc[1]
    }
    private val frameHeight by lazy { layout_root.height }

    private var selectorHeight: Float = 0.3f //ratio

    private var selectorDragStartBias: Float = -1f
    private var selectorDragStartTime: Long = -1

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

    override fun init(grid: ResourcesList) {
        Log.d(RESOURCES_SCREEN, "initializing ResourcesFragment")
        (activity as MainActivity).setSelectedTab(1)
        (activity as MainActivity).setToolbarVisibility(true)
        setHasOptionsMenu(true)

        initResources(grid)
        iv_drag_handler.setOnTouchListener(::dragHandlerTouchListener)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(RESOURCES_SCREEN, "options item selected in ResourcesFragment")
        when(item.itemId) {
            R.id.menu_tags_sort_by -> showSortByDialog()
            R.id.menu_tags_off -> disableTags()

            R.id.menu_tags_on -> {
                val tags = presenter.listTagsForAllResources()
                if (tags.isNotEmpty()) {
                    initResources(presenter.provideResourcesList())
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
        updateDragHandlerBias()
    }

    override fun setToolbarTitle(title: String) {
        (activity as MainActivity).setTitle(title)
    }

    override fun sortingValuesReceived() {
        sortAccordingToChoice()
    }

    override fun setProgressVisibility(isVisible: Boolean) {
        layout_progress.isVisible = isVisible
        (activity as MainActivity).setBottomNavigationEnabled(!isVisible)
    }

    override fun notifyUser(message: String, moreTime: Boolean) {
        Notifications.notifyUser(context, message, moreTime)
    }

    private fun initResources(resources: ResourcesList) {
        gridAdapter = ResourcesGrid(resources)

        rv_resources.adapter = gridAdapter
        rv_resources.layoutManager = GridLayoutManager(context, 3)

        tagsSelector = presenter.createTagsSelector()
        if (tagsSelector != null) {
            enableTags()
        } else {
            notifyUser("You don't have any tags here yet", moreTime = true)
            disableTags()
        }
    }

    //showing all resources, displaying tags selector
    private fun enableTags() {
        Log.d(RESOURCES_SCREEN, "enabling tags mode")

        tagsEnabled = true
        showTagsOnOffButtons()

        tags_cg.visibility = View.VISIBLE

        tagsSelector!!.drawChips(tags_cg, requireContext()) { selection ->
            notifyUser("${selection.size} resources selected")
            gridAdapter.updateItems(selection.toList())
        }
    }

    //showing only untagged resources, hiding tags selector
    private fun disableTags() {
        Log.d(RESOURCES_SCREEN, "disabling tags mode")

        tagsEnabled = false
        showTagsOnOffButtons()

        tags_cg.visibility = View.GONE

        val untagged = presenter.resources(untagged = true)
        gridAdapter.updateItems(untagged)
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
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_sort, null)!!
        val alertBuilder = AlertDialog.Builder(requireContext()).setView(view)

        changeSortOrderLockStatus(view, true)

        when(presenter.sorting) {
            Sorting.DEFAULT -> view.rb_default.isChecked = true
            Sorting.NAME -> view.rb_name.isChecked = true
            Sorting.SIZE -> view.rb_size.isChecked = true
            Sorting.LAST_MODIFIED -> view.rb_last_modified.isChecked = true
            Sorting.TYPE -> view.rb_type.isChecked = true
        }

        if (presenter.sorting == Sorting.DEFAULT) {
            changeSortOrderLockStatus(view, false)
        }
        else {
            if (presenter.sortOrderAscending) {
                view.rb_ascending.isChecked = true
            } else {
                view.rb_descending.isChecked = true
            }
        }

        var dialog: AlertDialog? = null

        view.rg_sorting.setOnCheckedChangeListener { _, checkedId ->

            presenter.sorting = (sortingCategorySelected(checkedId))

            Log.d(RESOURCES_SCREEN, "sorting criteria changed, sorting = ${presenter.sorting}")

            presenter.sortOrderAscending = true

            sortAccordingToChoice()
            dialog!!.dismiss()
        }

        view.rg_sorting_direction.setOnCheckedChangeListener { _, checkedId ->
            when(checkedId) {
                R.id.rb_ascending -> presenter.sortOrderAscending = true
                R.id.rb_descending -> presenter.sortOrderAscending = false
            }

            Log.d(RESOURCES_SCREEN, "sorting direction changed, ascending = ${presenter.sortOrderAscending}")

            gridAdapter.reverse()

            dialog!!.dismiss()
        }

        dialog = alertBuilder.show()
    }

    private fun sortAccordingToChoice(){
        when(presenter.sorting) {
            Sorting.NAME -> gridAdapter.sortBy { it.fileName }
            Sorting.SIZE -> gridAdapter.sortBy { Files.size(it) }
            Sorting.TYPE -> gridAdapter.sortBy { extension(it) }
            Sorting.LAST_MODIFIED -> gridAdapter.sortBy { Files.getLastModifiedTime(it) }
            Sorting.DEFAULT -> {
                sendShortToastMessage(requireActivity().getString(R.string.as_is_sorting_selected))
            }
        }

        Log.d("TAG", "sortAccordingToChoice: sorting: ${presenter.sorting}, reverse = ${presenter.sortOrderAscending}")
        if (!presenter.sortOrderAscending)
            gridAdapter.reverse()
    }

    private fun sortingCategorySelected(itemID: Int): Sorting{
        return when(itemID) {
            R.id.rb_default -> { Sorting.DEFAULT }
            R.id.rb_name -> Sorting.NAME
            R.id.rb_size -> Sorting.SIZE
            R.id.rb_last_modified -> Sorting.LAST_MODIFIED
            R.id.rb_type -> Sorting.TYPE
            else -> Sorting.DEFAULT
        }
    }

    private fun dragHandlerTouchListener(view: View, event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                val layoutParams = iv_drag_handler.layoutParams as ConstraintLayout.LayoutParams
                selectorDragStartBias = layoutParams.verticalBias
                selectorDragStartTime = System.currentTimeMillis()
            }
            MotionEvent.ACTION_UP -> {
                view.performClick()

                val travelTime = System.currentTimeMillis() - selectorDragStartTime
                val travelDelta = selectorDragStartBias - (1f - selectorHeight)
                val travelSpeed = 100f * travelDelta / (travelTime / 1000f)
                Log.d(RESOURCES_SCREEN, "draggable bar of tags selector was moved:")
                Log.d(RESOURCES_SCREEN, "delta=${100f * travelDelta}%")
                Log.d(RESOURCES_SCREEN, "time=${ 100f * travelTime }%")
                Log.d(RESOURCES_SCREEN, "speed=${100f * travelSpeed}%")

                if (travelTime > DRAG_TRAVEL_TIME_THRESHOLD &&
                    abs(travelDelta) > DRAG_TRAVEL_DELTA_THRESHOLD &&
                    abs(travelSpeed) > DRAG_TRAVEL_SPEED_THRESHOLD) {
                    selectorHeight = if (travelDelta > 0f) 1f else 0f
                    updateDragHandlerBias()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val distanceFromTop = event.rawY - frameTop
                selectorHeight = if (distanceFromTop < 0f) {
                    1f
                } else if (distanceFromTop > frameHeight) {
                    0f
                } else {
                    1f - distanceFromTop / frameHeight
                }

                updateVerticalBias(view)
            }
        }
        return true
    }

    private fun updateVerticalBias(view: View) {
        val layoutParams = view.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.verticalBias = 1f - selectorHeight
        view.layoutParams = layoutParams
    }

    private fun updateDragHandlerBias() {
        updateVerticalBias(iv_drag_handler)
    }

    private fun sendShortToastMessage(message: String){
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun changeSortOrderLockStatus(dialogView: View, isEnabledStatus: Boolean){
        val childCount = dialogView.rg_sorting_direction.childCount
        for (radioButton in 0 until childCount){
            dialogView.rg_sorting_direction?.getChildAt(radioButton)?.changeEnabledStatus(isEnabledStatus)
        }
    }

    companion object {
        private const val DRAG_TRAVEL_TIME_THRESHOLD = 50      //milliseconds
        private const val DRAG_TRAVEL_DELTA_THRESHOLD = 0.1    //ratio
        private const val DRAG_TRAVEL_SPEED_THRESHOLD = 150    //percents per second
    }
}