package space.taran.arknavigator.ui.fragments

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.*
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.GridLayoutManager
import moxy.MvpAppCompatFragment
import moxy.ktx.moxyPresenter
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.DialogSortBinding
import space.taran.arknavigator.databinding.FragmentResourcesBinding
import space.taran.arknavigator.mvp.model.repo.RootAndFav
import space.taran.arknavigator.mvp.presenter.ResourcesPresenter
import space.taran.arknavigator.mvp.view.ResourcesView
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.ui.activity.MainActivity
import space.taran.arknavigator.ui.adapter.ResourcesRVAdapter
import space.taran.arknavigator.ui.adapter.TagsSelectorAdapter
import space.taran.arknavigator.ui.fragments.utils.Notifications
import space.taran.arknavigator.utils.FullscreenHelper
import space.taran.arknavigator.utils.RESOURCES_SCREEN
import space.taran.arknavigator.utils.Sorting
import space.taran.arknavigator.utils.extensions.changeEnabledStatus
import space.taran.arknavigator.utils.extensions.closeKeyboard
import space.taran.arknavigator.utils.extensions.placeCursorToEnd
import space.taran.arknavigator.utils.extensions.showKeyboard
import kotlin.math.abs


//`root` is used for querying tags storage and resources index,
//       if it is `null`, then resources from all roots are taken
//                        and tags storage for every particular resource
//                        is determined dynamically
//
//`path` is used for filtering resources' paths
//       if it is `null`, then no filtering is performed
//       (recommended instead of passing same value for `path` and `root)
class ResourcesFragment : MvpAppCompatFragment(), ResourcesView {

    private val presenter by moxyPresenter {
        ResourcesPresenter(requireArguments()[ROOT_AND_FAV_KEY] as RootAndFav).apply {
            Log.d(RESOURCES_SCREEN, "creating ResourcesPresenter")
            App.instance.appComponent.inject(this)
        }
    }

    private lateinit var binding: FragmentResourcesBinding
    private var resourcesAdapter: ResourcesRVAdapter? = null
    private var sortByDialog: AlertDialog? = null

    private val frameTop by lazy {
        val loc = IntArray(2)
        binding.root.getLocationOnScreen(loc)
        loc[1]
    }
    private val frameHeight by lazy { binding.root.height }

    private var selectorHeight: Float = 0.3f //ratio

    private var selectorDragStartBias: Float = -1f
    private var selectorDragStartTime: Long = -1

    private var tagsSelectorAdapter: TagsSelectorAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        Log.d(RESOURCES_SCREEN, "inflating layout for ResourcesFragment")
        binding = FragmentResourcesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(RESOURCES_SCREEN, "view created in ResourcesFragment")
        super.onViewCreated(view, savedInstanceState)

        App.instance.appComponent.inject(this)
    }

    override fun init() {
        Log.d(RESOURCES_SCREEN, "initializing ResourcesFragment")

        initResultListeners()

        FullscreenHelper.setStatusBarVisibility(true, requireActivity().window)
        (activity as MainActivity).setSelectedTab(1)
        (activity as MainActivity).setToolbarVisibility(true)
        (requireActivity() as MainActivity).setBottomNavigationVisibility(true)
        setHasOptionsMenu(true)

        resourcesAdapter = ResourcesRVAdapter(presenter.gridPresenter)
        binding.rvResources.adapter = resourcesAdapter
        binding.rvResources.layoutManager = GridLayoutManager(context, 3)

        tagsSelectorAdapter = TagsSelectorAdapter(
            binding.cgTagsChecked,
            binding.tagsCg,
            presenter.tagsSelectorPresenter
        )
        binding.ivDragHandler.setOnTouchListener(::dragHandlerTouchListener)
        binding.etTagsFilter.doAfterTextChanged {
            presenter.tagsSelectorPresenter.onFilterChanged(it.toString())
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            presenter.onBackClick()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(RESOURCES_SCREEN, "options item selected in ResourcesFragment")
        when (item.itemId) {
            R.id.menu_tags_sort_by -> presenter.onMenuSortDialogClick()
            R.id.menu_tags_off -> presenter.onMenuTagsToggle(false)
            R.id.menu_tags_on -> presenter.onMenuTagsToggle(true)
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_tags_on).isVisible = !presenter.tagsEnabled
        menu.findItem(R.id.menu_tags_off).isVisible = presenter.tagsEnabled
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        Log.d(RESOURCES_SCREEN, "inflating options menu in ResourcesFragment")
        inflater.inflate(R.menu.menu_tags_screen, menu)
    }

    override fun onResume() {
        Log.d(RESOURCES_SCREEN, "resuming in ResourcesFragment")
        super.onResume()
        updateDragHandlerBias()
    }

    override fun setToolbarTitle(title: String) {
        (activity as MainActivity).setTitle(title)
    }

    override fun setProgressVisibility(isVisible: Boolean, withText: String) {
        binding.layoutProgress.apply {
            root.isVisible = isVisible
            (activity as MainActivity).setBottomNavigationEnabled(!isVisible)

            if (withText.isNotEmpty()) {
                progressText.setVisibilityAndLoadingStatus(View.VISIBLE)
                progressText.loadingText = withText
            } else {
                progressText.setVisibilityAndLoadingStatus(View.GONE)
            }
        }
    }

    override fun updateAdapter() {
        resourcesAdapter?.notifyDataSetChanged()
    }

    override fun notifyUser(message: String, moreTime: Boolean) {
        if (isFragmentVisible()) Notifications.notifyUser(context, message, moreTime)
    }

    override fun setTagsEnabled(enabled: Boolean) {
        requireActivity().invalidateOptionsMenu()
        binding.layoutTags.isVisible = enabled
        binding.ivDragHandler.isVisible = enabled
        if (enabled) {
            val constraintSet = ConstraintSet()
            constraintSet.clone(binding.root)
            constraintSet.connect(
                binding.rvResources.id,
                ConstraintSet.BOTTOM,
                binding.ivDragHandler.id,
                ConstraintSet.TOP
            )
            constraintSet.applyTo(binding.root)
        } else {
            val constraintSet = ConstraintSet()
            constraintSet.clone(binding.root)
            constraintSet.connect(
                binding.rvResources.id,
                ConstraintSet.BOTTOM,
                binding.root.id,
                ConstraintSet.BOTTOM
            )
            constraintSet.applyTo(binding.root)
        }
    }

    override fun setTagsSelectorHintEnabled(enabled: Boolean) {
        binding.tvTagsSelectorHint.isVisible = enabled
    }

    override fun setTagsFilterEnabled(enabled: Boolean) {
        binding.layoutInput.isVisible = enabled
        binding.cgTagsChecked.isVisible = enabled
        if (enabled) {
            binding.etTagsFilter.placeCursorToEnd()
            binding.etTagsFilter.showKeyboard()
        } else
            binding.etTagsFilter.closeKeyboard()
    }

    override fun setTagsFilterText(filter: String) {
        binding.etTagsFilter.setText(filter)
    }

    override fun drawTags() {
        tagsSelectorAdapter?.drawTags()
    }

    override fun showSortDialog(sorting: Sorting, ascending: Boolean) {
        showSortByDialog(sorting, ascending)
    }

    override fun closeSortDialog() {
        sortByDialog?.dismiss()
    }

    private fun initResultListeners() {
        setFragmentResultListener(GalleryFragment.REQUEST_TAGS_CHANGED_KEY) { _, _ ->
            presenter.onResourcesOrTagsChanged()
        }
        setFragmentResultListener(GalleryFragment.REQUEST_RESOURCES_CHANGED_KEY) { _, _ ->
            presenter.onResourcesOrTagsChanged()
        }
    }

    private fun showSortByDialog(sorting: Sorting, ascending: Boolean) {
        Log.d(RESOURCES_SCREEN, "showing sort-by dialog in ResourcesFragment")
        val dialogBinding = DialogSortBinding.inflate(LayoutInflater.from(requireContext()))

        val alertBuilder = AlertDialog.Builder(requireContext()).setView(dialogBinding.root)

        changeSortOrderEnabledStatus(dialogBinding, true)

        dialogBinding.apply {
            when (sorting) {
                Sorting.DEFAULT -> rbDefault.isChecked = true
                Sorting.NAME -> rbName.isChecked = true
                Sorting.SIZE -> rbSize.isChecked = true
                Sorting.LAST_MODIFIED -> rbLastModified.isChecked = true
                Sorting.TYPE -> rbType.isChecked = true
            }

            if (sorting == Sorting.DEFAULT) {
                changeSortOrderEnabledStatus(dialogBinding, false)
            } else {
                if (ascending) {
                    rbAscending.isChecked = true
                } else {
                    rbDescending.isChecked = true
                }
            }

            rgSorting.setOnCheckedChangeListener { _, checkedId ->

                val newSorting = sortingCategorySelected(checkedId)

                Log.d(RESOURCES_SCREEN, "sorting criteria changed, sorting = $newSorting")

                if (newSorting == Sorting.DEFAULT)
                    notifyUser(requireActivity().getString(R.string.as_is_sorting_selected))


                presenter.gridPresenter.updateSorting(newSorting)
                presenter.onSortDialogClose()
            }

            rgSortingDirection.setOnCheckedChangeListener { _, checkedId ->
                var newAscending = false
                when (checkedId) {
                    R.id.rb_ascending -> newAscending = true
                    R.id.rb_descending -> newAscending = false
                }

                Log.d(RESOURCES_SCREEN, "sorting direction changed, ascending = $newAscending")

                presenter.gridPresenter.updateAscending(newAscending)

                presenter.onSortDialogClose()
            }

            alertBuilder.setOnDismissListener {
                presenter.onSortDialogClose()
            }

            sortByDialog = alertBuilder.show()
        }
    }

    private fun sortingCategorySelected(itemID: Int): Sorting {
        return when (itemID) {
            R.id.rb_default -> {
                Sorting.DEFAULT
            }
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
                val layoutParams =
                    binding.ivDragHandler.layoutParams as ConstraintLayout.LayoutParams
                selectorDragStartBias = layoutParams.verticalBias
                selectorDragStartTime = SystemClock.uptimeMillis()
            }
            MotionEvent.ACTION_UP -> {
                view.performClick()

                val travelTime = SystemClock.uptimeMillis() - selectorDragStartTime
                val travelDelta = selectorDragStartBias - (1f - selectorHeight)
                val travelSpeed = 100f * travelDelta / (travelTime / 1000f)
                Log.d(RESOURCES_SCREEN, "draggable bar of tags selector was moved:")
                Log.d(RESOURCES_SCREEN, "delta=${100f * travelDelta}%")
                Log.d(RESOURCES_SCREEN, "time=${travelTime}ms")
                Log.d(RESOURCES_SCREEN, "speed=${travelSpeed}%/sec")

                if (travelTime > DRAG_TRAVEL_TIME_THRESHOLD &&
                    abs(travelDelta) > DRAG_TRAVEL_DELTA_THRESHOLD &&
                    abs(travelSpeed) > DRAG_TRAVEL_SPEED_THRESHOLD
                ) {
                    selectorHeight = if (travelDelta > 0f) {
                        presenter.tagsSelectorPresenter.onFilterToggle(true)
                        1f
                    } else {
                        presenter.tagsSelectorPresenter.onFilterToggle(false)
                        0f
                    }
                    updateDragHandlerBias()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val distanceFromTop = event.rawY - frameTop
                selectorHeight = if (distanceFromTop < 0f) {
                    presenter.tagsSelectorPresenter.onFilterToggle(true)
                    1f
                } else if (distanceFromTop > frameHeight) {
                    0f
                } else {
                    presenter.tagsSelectorPresenter.onFilterToggle(false)
                    1f - distanceFromTop / frameHeight
                }

                val newBias = updateVerticalBias(view)

                val historySize = event.historySize
                if (historySize >= 2) {
                    val oldest = event.getHistoricalY(historySize - 2)
                    val old = event.getHistoricalY(historySize - 1)

                    val turnedFromDownToUp = event.y < old && old > oldest
                    val turnedFromUpToDown = event.y > old && old < oldest

                    if (turnedFromDownToUp || turnedFromUpToDown) {
                        selectorDragStartBias = newBias
                        selectorDragStartTime = SystemClock.uptimeMillis()
                    }
                }
            }
        }
        return true
    }

    private fun updateVerticalBias(view: View): Float {
        val layoutParams = view.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.verticalBias = 1f - selectorHeight
        view.layoutParams = layoutParams

        return layoutParams.verticalBias
    }

    private fun updateDragHandlerBias() {
        updateVerticalBias(binding.ivDragHandler)
    }

    /**
     * ResourcesFragment can be overlapped by GalleryFragment
     */
    private fun isFragmentVisible(): Boolean {
        return parentFragmentManager.fragments.find { f -> f is GalleryFragment } == null
    }

    private fun changeSortOrderEnabledStatus(
        dialogBinding: DialogSortBinding,
        isEnabledStatus: Boolean
    ) {
        val childCount = dialogBinding.rgSortingDirection.childCount
        for (radioButton in 0 until childCount) {
            dialogBinding.rgSortingDirection.getChildAt(radioButton)
                .changeEnabledStatus(isEnabledStatus)
        }
    }

    companion object {
        private const val DRAG_TRAVEL_TIME_THRESHOLD = 30      //milliseconds
        private const val DRAG_TRAVEL_DELTA_THRESHOLD = 0.1    //ratio
        private const val DRAG_TRAVEL_SPEED_THRESHOLD = 150    //percents per second
        private const val ROOT_AND_FAV_KEY = "rootAndFav"

        fun newInstance(rootAndFav: RootAndFav) = ResourcesFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ROOT_AND_FAV_KEY, rootAndFav)
            }
        }
    }
}