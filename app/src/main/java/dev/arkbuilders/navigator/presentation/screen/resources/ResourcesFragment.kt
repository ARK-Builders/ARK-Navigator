package dev.arkbuilders.navigator.presentation.screen.resources

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.launch
import moxy.MvpAppCompatFragment
import moxy.ktx.moxyPresenter
import moxy.presenterScope
import space.taran.arkfilepicker.folders.RootAndFav
import space.taran.arkfilepicker.presentation.onArkPathPicked
import space.taran.arklib.ResourceId
import dev.arkbuilders.navigator.BuildConfig
import dev.arkbuilders.navigator.R
import dev.arkbuilders.navigator.databinding.FragmentResourcesBinding
import dev.arkbuilders.navigator.presentation.screen.resources.tagsselector.QueryMode
import dev.arkbuilders.navigator.presentation.App
import dev.arkbuilders.navigator.presentation.screen.main.MainActivity
import dev.arkbuilders.navigator.presentation.screen.resources.adapter.ResourcesRVAdapter
import dev.arkbuilders.navigator.presentation.screen.resources.tagsselector.TagsSelectorAdapter
import dev.arkbuilders.navigator.presentation.screen.gallery.GalleryFragment
import dev.arkbuilders.navigator.presentation.dialog.ConfirmationDialogFragment
import dev.arkbuilders.navigator.presentation.dialog.sort.SortDialogFragment
import dev.arkbuilders.navigator.presentation.dialog.StorageExceptionDialogFragment
import dev.arkbuilders.navigator.presentation.dialog.tagssort.TagsSortDialogFragment
import dev.arkbuilders.navigator.presentation.utils.toast
import dev.arkbuilders.navigator.presentation.utils.toastFailedPaths
import dev.arkbuilders.navigator.presentation.view.StackedToasts
import dev.arkbuilders.navigator.presentation.utils.FullscreenHelper
import dev.arkbuilders.navigator.data.utils.LogTags.RESOURCES_SCREEN
import space.taran.arklib.domain.tags.Tag
import dev.arkbuilders.navigator.presentation.utils.closeKeyboard
import dev.arkbuilders.navigator.presentation.utils.placeCursorToEnd
import dev.arkbuilders.navigator.presentation.utils.showKeyboard
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.math.abs

// `root` is used for querying tags storage and resources index,
//       if it is `null`, then resources from all roots are taken
//                        and tags storage for every particular resource
//                        is determined dynamically
//
// `path` is used for filtering resources' paths
//       if it is `null`, then no filtering is performed
//       (recommended instead of passing same value for `path` and `root)
class ResourcesFragment : MvpAppCompatFragment(), ResourcesView {

    val presenter by moxyPresenter {
        ResourcesPresenter(
            requireArguments()[ROOT_AND_FAV_KEY] as RootAndFav,
            requireArguments().getString(SELECTED_TAG_KEY)
        ).apply {
            Log.d(RESOURCES_SCREEN, "creating ResourcesPresenter")
            App.instance.appComponent.inject(this)
        }
    }

    private lateinit var binding: FragmentResourcesBinding
    private var resourcesAdapter: ResourcesRVAdapter? = null
    private lateinit var stackedToasts: StackedToasts

    private val frameTop by lazy {
        val loc = IntArray(2)
        binding.root.getLocationOnScreen(loc)
        loc[1]
    }
    private val frameHeight by lazy { binding.root.height }

    private var selectorHeight: Float = 0.3f // ratio

    private var selectorDragStartBias: Float = -1f
    private var selectorDragStartTime: Long = -1

    private var tagsSelectorAdapter: TagsSelectorAdapter? = null

    private var isShuffled = false
    private var isAscending = true
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

    override fun init(ascending: Boolean, sortByScoresEnabled: Boolean) =
        with(binding) {
            Log.d(RESOURCES_SCREEN, "initializing ResourcesFragment")

            initResultListeners()
            initMenuListeners()
            stackedToasts = StackedToasts(binding.rvToasts, lifecycleScope)

            FullscreenHelper.setStatusBarVisibility(true, requireActivity().window)
            (activity as MainActivity).setSelectedTab(R.id.page_tags)
            (requireActivity() as MainActivity).setBottomNavigationVisibility(true)

            tagsSelectorAdapter = TagsSelectorAdapter(
                this@ResourcesFragment,
                binding,
                presenter.tagsSelectorPresenter
            ).also {
                App.instance.appComponent.inject(it)
            }

            layoutDragHandler.setOnTouchListener(::dragHandlerTouchListener)
            etTagsFilter.doAfterTextChanged {
                presenter.tagsSelectorPresenter.onFilterChanged(it.toString())
            }
            switchKind.setOnCheckedChangeListener { _, checked ->
                presenter.tagsSelectorPresenter.onKindTagsToggle(checked)
            }
            btnClear.setOnClickListener {
                presenter.tagsSelectorPresenter.onClearClick()
            }
            btnTagsSorting.setOnClickListener {
                TagsSortDialogFragment
                    .newInstance(selectorNotEdit = true)
                    .show(childFragmentManager, null)
            }

            this@ResourcesFragment.updateOrderBtn(ascending)

            if (sortByScoresEnabled) {
                switchScores.isChecked = true
                switchScores.jumpDrawablesToCurrentState()
            }
            switchScores.setOnCheckedChangeListener { _, isChecked ->
                Log.d(
                    RESOURCES_SCREEN,
                    "sorting by scores ${if (isChecked) "enabled" else "disabled"}"
                )
                presenter.onScoresSwitched(isChecked)
            }

            this@ResourcesFragment
                .requireActivity()
                .onBackPressedDispatcher
                .addCallback(this@ResourcesFragment) {
                    presenter.onBackClick()
                }
            return@with
        }

    override fun initResourcesAdapter() =
        with(binding) {
            resourcesAdapter = ResourcesRVAdapter(presenter.gridPresenter)
            rvResources.adapter = resourcesAdapter
            rvResources.setItemViewCacheSize(0)
            rvResources.layoutManager = GridLayoutManager(context, 3)
        }

    override fun onResume() {
        Log.d(RESOURCES_SCREEN, "resuming in ResourcesFragment")
        super.onResume()
        updateDragHandlerBias()
    }

    override fun setToolbarTitle(title: String) {
        binding.actionBar.tvTitle.text = title
    }

    override fun setKindTagsEnabled(enabled: Boolean) {
        binding.switchKind.toggleSwitchSilent(enabled)
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

    override fun updateResourcesAdapter() {
        resourcesAdapter?.notifyDataSetChanged()
    }

    override fun updateMenu() = with(binding) {
        val queryMode = presenter.tagsSelectorPresenter.queryMode
        val normalVisibility =
            if (queryMode != QueryMode.NORMAL) View.VISIBLE else View.INVISIBLE
        val focusVisibility =
            if (queryMode != QueryMode.FOCUS) View.VISIBLE else View.INVISIBLE
        actionBar.btnNormalMode.visibility = normalVisibility
        actionBar.btnFocusMode.visibility = focusVisibility
        return@with
    }

    override fun updateOrderBtn(isAscending: Boolean) = with(binding) {
        val ctx = requireContext()
        this@ResourcesFragment.isAscending = isAscending

        val drawable = if (isAscending) {
            ctx.getDrawable(R.drawable.order_ascending)
        } else {
            ctx.getDrawable(R.drawable.order_descending)
        }

        actionBar.btnOrder.setImageDrawable(drawable)
        return@with
    }

    override fun setSelectingEnabled(enabled: Boolean) = with(binding.actionBar) {
        tvTitle.isVisible = !enabled
        tvSelectedOf.isVisible = enabled
        ivDisableSelectionMode.isVisible = enabled
        ivUseSelected.isVisible = enabled

        return@with
    }

    override fun setSelectingCount(selected: Int, all: Int) {
        binding.actionBar.tvSelectedOf.text = "$selected of $all"
    }

    override fun setTagsFilterEnabled(enabled: Boolean) {
        binding.layoutInput.isVisible = enabled
        binding.rvTagsFilter.isVisible = enabled
        if (enabled) {
            binding.etTagsFilter.placeCursorToEnd()
            binding.etTagsFilter.showKeyboard()
        } else
            binding.etTagsFilter.closeKeyboard()
    }

    override fun setTagsFilterText(filter: String) {
        binding.etTagsFilter.setText(filter)
    }

    override fun setTagsSortingVisibility(
        visible: Boolean
    ) {
        binding.btnTagsSorting.isVisible = visible
        val params = binding.btnClear.layoutParams as ConstraintLayout.LayoutParams
        params.horizontalBias = if (visible) 1f else 0.5f
        binding.btnClear.layoutParams = params
    }

    override fun drawTags() {
        tagsSelectorAdapter?.drawTags()
    }

    override fun toastResourcesSelected(selected: Int) {
        if (isFragmentVisible())
            toast(R.string.toast_resources_selected, selected)
    }

    override fun toastResourcesSelectedFocusMode(selected: Int, hidden: Int) {
        if (isFragmentVisible())
            toast(
                R.string.toast_resources_selected_focus_mode,
                selected,
                hidden
            )
    }

    override fun toastPathsFailed(failedPaths: List<Path>) =
        toastFailedPaths(failedPaths)

    override fun clearStackedToasts() {
        stackedToasts.clearToasts()
    }

    override fun onSelectingChanged(enabled: Boolean) {
        binding.rvResources.recycledViewPool.clear()
        resourcesAdapter?.onSelectingChanged(enabled)
    }

    override fun shareResources(resources: List<Path>) {
        val fileUris = resources.map {
            FileProvider.getUriForFile(
                requireContext(),
                BuildConfig.APPLICATION_ID + ".provider",
                it.toFile()
            )
        }
        val intent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            type = "file/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(fileUris))
        }
        startActivity(
            Intent.createChooser(
                intent,
                getString(R.string.share_resources_with)
            )
        )
    }

    override fun displayStorageException(label: String, msg: String) {
        StorageExceptionDialogFragment.newInstance(label, msg)
            .show(
                childFragmentManager,
                StorageExceptionDialogFragment.TAG
            )
    }

    override fun setPreviewGenerationProgress(isVisible: Boolean) {
        binding.progressPreviewGeneration.isVisible = isVisible
    }

    override fun setMetadataExtractionProgress(isVisible: Boolean) {
        binding.progressMetadataExtraction.isVisible = isVisible
    }

    private fun initMenuListeners() = with(binding) {
        actionBar.ivDisableSelectionMode.setOnClickListener {
            presenter.gridPresenter.onSelectingChanged(false)
        }
        actionBar.ivUseSelected.setOnClickListener {
            setupAndShowSelectedResourcesMenu(it)
        }
        actionBar.btnSort.setOnClickListener {
            val dialog = SortDialogFragment.newInstance()
            dialog.show(childFragmentManager, null)
        }
        actionBar.btnNormalMode.setOnClickListener {
            presenter.tagsSelectorPresenter.onQueryModeChanged(QueryMode.NORMAL)
        }
        actionBar.btnFocusMode.setOnClickListener {
            presenter.tagsSelectorPresenter.onQueryModeChanged(QueryMode.FOCUS)
        }
        actionBar.btnOrder.apply {
            setOnClickListener {
                presenter.onAscendingChanged(!isAscending)
            }
        }
        actionBar.btnShuffle.apply {
            setOnClickListener {
                val dice = this.drawable

                @ColorInt
                val diceColor = resources
                    .getColor(R.color.blue, requireContext().theme)
                isShuffled = !isShuffled
                if (isShuffled) {
                    dice?.setTint(diceColor)
                    presenter.onShuffleSwitchedOn()
                } else {
                    dice?.setTintList(null)
                    presenter.onShuffleSwitchedOff()
                }
            }
        }
    }

    private fun initResultListeners() {
        childFragmentManager.onArkPathPicked(
            this,
            MOVE_SELECTED_REQUEST_KEY
        ) { path ->
            val selectedSize = presenter.gridPresenter.resources
                .filter { it.isSelected }
                .size
            val description = "$selectedSize " +
                getString(R.string.resources_will_be_moved)
            ConfirmationDialogFragment
                .newInstance(
                    getString(R.string.are_you_sure),
                    description,
                    getString(R.string.yes),
                    getString(R.string.no),
                    MOVE_CONFIRMATION_REQUEST_KEY,
                    bundleOf(MOVE_TO_PATH_KEY to path.toString())
                )
                .show(parentFragmentManager, null)
        }

        childFragmentManager.onArkPathPicked(
            this,
            COPY_SELECTED_REQUEST_KEY
        ) {
            presenter.onCopySelectedResourcesClicked(it)
        }

        setFragmentResultListener(
            MOVE_CONFIRMATION_REQUEST_KEY
        ) { _, bundle ->
            presenter.onMoveSelectedResourcesClicked(
                Path(
                    bundle
                        .getString(MOVE_TO_PATH_KEY)!!
                )
            )
        }

        setFragmentResultListener(
            DELETE_CONFIRMATION_REQUEST_KEY
        ) { _, _ ->
            presenter.onRemoveSelectedResourcesClicked()
        }

        setFragmentResultListener(
            GalleryFragment.REQUEST_TAGS_CHANGED_KEY
        ) { _, _ ->
            presenter.apply {
                presenterScope.launch {
                    onTagsChanged()
                }
            }
        }

        setFragmentResultListener(
            GalleryFragment.REQUEST_RESOURCES_CHANGED_KEY
        ) { _, _ ->
            presenter.apply {
                presenterScope.launch {
                    onResourcesOrTagsChanged()
                }
            }
        }

        setFragmentResultListener(
            RESET_SCORES_FOR_SELECTED
        ) { _, _ ->
            presenter.onResetScoresClicked()
        }

        setFragmentResultListener(
            GalleryFragment.SCORES_CHANGED_KEY
        ) { _, _ ->
            presenter.gridPresenter.onScoresChangedExternally()
        }

        setFragmentResultListener(
            GalleryFragment.SELECTED_CHANGED_KEY
        ) { _, bundle ->
            val selectingEnabled = bundle.getBoolean(
                GalleryFragment.SELECTING_ENABLED_KEY
            )
            presenter.gridPresenter.onSelectingChanged(selectingEnabled)
            if (selectingEnabled) {
                presenter.gridPresenter.onSelectedChangedExternally(
                    bundle.getParcelableArray(
                        GalleryFragment.SELECTED_RESOURCES_KEY
                    )!!.toList() as List<ResourceId>
                )
            }
        }

        childFragmentManager.setFragmentResultListener(
            StorageExceptionDialogFragment.STORAGE_CORRUPTION_DETECTED,
            this
        ) { _, _ ->
            presenter.onBackClick()
        }
    }

    private fun dragHandlerTouchListener(view: View, event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                val layoutParams = binding.ivDragHandler.layoutParams
                    as ConstraintLayout.LayoutParams
                selectorDragStartBias = layoutParams.verticalBias
                selectorDragStartTime = SystemClock.uptimeMillis()
            }
            MotionEvent.ACTION_UP -> {
                view.performClick()

                val travelTime =
                    SystemClock.uptimeMillis() - selectorDragStartTime
                val travelDelta = selectorDragStartBias - (1f - selectorHeight)
                val travelSpeed = 100f * travelDelta / (travelTime / 1000f)
                Log.d(
                    RESOURCES_SCREEN,
                    "draggable bar of tags selector was moved:"
                )
                Log.d(RESOURCES_SCREEN, "delta=${100f * travelDelta}%")
                Log.d(RESOURCES_SCREEN, "time=${travelTime}ms")
                Log.d(RESOURCES_SCREEN, "speed=$travelSpeed%/sec")

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
        updateVerticalBias(binding.layoutDragHandler)
    }

    /**
     * ResourcesFragment can be overlapped by GalleryFragment
     */
    private fun isFragmentVisible(): Boolean {
        return parentFragmentManager.fragments.find { f ->
            f is GalleryFragment
        } == null
    }

    override fun toastIndexFailedPath(path: Path) {
        stackedToasts.toast(path)
    }

    companion object {
        const val MOVE_SELECTED_REQUEST_KEY = "moveSelected"
        const val COPY_SELECTED_REQUEST_KEY = "copySelected"
        private const val MOVE_CONFIRMATION_REQUEST_KEY = "moveConfirm"
        const val DELETE_CONFIRMATION_REQUEST_KEY = "deleteConfirm"
        private const val MOVE_TO_PATH_KEY = "moveToPath"
        const val RESET_SCORES_FOR_SELECTED = "resetSelectedScores"
        const val STORAGE_CORRUPTION_DETECTED = "storage corrupted"

        private const val DRAG_TRAVEL_TIME_THRESHOLD = 30 // milliseconds
        private const val DRAG_TRAVEL_DELTA_THRESHOLD = 0.1 // ratio
        private const val DRAG_TRAVEL_SPEED_THRESHOLD =
            150 // percents per second
        private const val ROOT_AND_FAV_KEY = "rootAndFav"
        private const val SELECTED_TAG_KEY = "selectedTag"

        fun newInstance(
            rootAndFav: RootAndFav
        ) = ResourcesFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ROOT_AND_FAV_KEY, rootAndFav)
            }
        }

        fun newInstanceWithSelectedTag(
            rootAndFav: RootAndFav,
            tag: Tag
        ) = ResourcesFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ROOT_AND_FAV_KEY, rootAndFav)
                putString(SELECTED_TAG_KEY, tag)
            }
        }
    }
}
