package space.taran.arknavigator.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import moxy.MvpAppCompatFragment
import moxy.ktx.moxyPresenter
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.FragmentResourcesBinding
import space.taran.arknavigator.mvp.model.repo.RootAndFav
import space.taran.arknavigator.mvp.presenter.ResourcesPresenter
import space.taran.arknavigator.mvp.view.ResourcesView
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.ui.activity.MainActivity
import space.taran.arknavigator.ui.adapter.ResourcesRVAdapter
import space.taran.arknavigator.ui.adapter.TagsSelectorAdapter
import space.taran.arknavigator.ui.fragments.dialog.SortDialogFragment
import space.taran.arknavigator.ui.fragments.utils.Notifications
import space.taran.arknavigator.utils.FullscreenHelper
import space.taran.arknavigator.utils.RESOURCES_SCREEN
import space.taran.arknavigator.utils.extensions.closeKeyboard
import space.taran.arknavigator.utils.extensions.placeCursorToEnd
import space.taran.arknavigator.utils.extensions.showKeyboard

// `root` is used for querying tags storage and resources index,
//       if it is `null`, then resources from all roots are taken
//                        and tags storage for every particular resource
//                        is determined dynamically
//
// `path` is used for filtering resources' paths
//       if it is `null`, then no filtering is performed
//       (recommended instead of passing same value for `path` and `root)
class ResourcesFragment : MvpAppCompatFragment(), ResourcesView {

    private val presenter by moxyPresenter {
        ResourcesPresenter(requireArguments()[ROOT_AND_FAV_KEY] as RootAndFav)
            .apply {
                Log.d(RESOURCES_SCREEN, "creating ResourcesPresenter")
                App.instance.appComponent.inject(this)
            }
    }

    private lateinit var binding: FragmentResourcesBinding
    private var resourcesAdapter: ResourcesRVAdapter? = null

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
        (activity as MainActivity).setSelectedTab(R.id.page_tags)
        (activity as MainActivity).setToolbarVisibility(true)
        (requireActivity() as MainActivity).setBottomNavigationVisibility(true)
        setHasOptionsMenu(true)

        resourcesAdapter = ResourcesRVAdapter(presenter.gridPresenter)
        binding.rvResources.adapter = resourcesAdapter
        binding.rvResources.layoutManager = GridLayoutManager(context, 3)

        val bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)

        val bottomSheetCallback =
            object : BottomSheetBehavior.BottomSheetCallback() {

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_EXPANDED ->
                            presenter.tagsSelectorPresenter.onFilterToggle(true)
                        else ->
                            presenter.tagsSelectorPresenter.onFilterToggle(false)
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }
            }

        // To add the callback:
        bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)

        tagsSelectorAdapter = TagsSelectorAdapter(
            binding.cgTagsChecked,
            binding.tagsCg,
            presenter.tagsSelectorPresenter
        )

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
            R.id.menu_tags_sort_by -> {
                val dialog = SortDialogFragment.newInstance()
                dialog.show(childFragmentManager, null)
            }
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
        if (isFragmentVisible()) Notifications.notifyUser(
            context,
            message,
            moreTime
        )
    }

    override fun notifyUser(messageID: Int, moreTime: Boolean) {
        Notifications.notifyUser(context, messageID, moreTime)
    }

    override fun setTagsEnabled(enabled: Boolean) {
        requireActivity().invalidateOptionsMenu()
        binding.layoutTags.isVisible = enabled
        binding.ivDragHandler.isVisible = enabled
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

    private fun initResultListeners() {
        setFragmentResultListener(
            GalleryFragment.REQUEST_TAGS_CHANGED_KEY
        ) { _, _ ->
            presenter.onResourcesOrTagsChanged()
        }

        setFragmentResultListener(
            GalleryFragment.REQUEST_RESOURCES_CHANGED_KEY
        ) { _, _ ->
            presenter.onResourcesOrTagsChanged()
        }
    }

    /**
     * ResourcesFragment can be overlapped by GalleryFragment
     */
    private fun isFragmentVisible(): Boolean {
        return parentFragmentManager.fragments.find { f ->
            f is GalleryFragment
        } == null
    }

    companion object {
        private const val ROOT_AND_FAV_KEY = "rootAndFav"

        fun newInstance(rootAndFav: RootAndFav) = ResourcesFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ROOT_AND_FAV_KEY, rootAndFav)
            }
        }
    }
}
