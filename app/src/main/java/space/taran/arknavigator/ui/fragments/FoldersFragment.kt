package space.taran.arknavigator.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import moxy.MvpAppCompatFragment
import moxy.ktx.moxyPresenter
import space.taran.arknavigator.databinding.FragmentFoldersBinding
import space.taran.arknavigator.mvp.presenter.FoldersPresenter
import space.taran.arknavigator.mvp.view.FoldersView
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.ui.activity.MainActivity
import space.taran.arknavigator.ui.adapter.folderstree.FoldersTreeAdapter
import space.taran.arknavigator.ui.fragments.dialog.FolderPickerDialogFragment
import space.taran.arknavigator.ui.fragments.utils.Notifications
import space.taran.arknavigator.utils.FOLDERS_SCREEN
import space.taran.arknavigator.utils.FullscreenHelper
import java.nio.file.Path
import kotlin.io.path.Path

class FoldersFragment : MvpAppCompatFragment(), FoldersView {
    private var foldersTreeAdapter: FoldersTreeAdapter? = null

    private lateinit var binding: FragmentFoldersBinding
    private val presenter by moxyPresenter {
        FoldersPresenter().apply {
            Log.d(FOLDERS_SCREEN, "RootsPresenter created")
            App.instance.appComponent.inject(this)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(FOLDERS_SCREEN, "inflating layout for FoldersFragment")
        binding = FragmentFoldersBinding.inflate(inflater, container, false)
        FullscreenHelper.setStatusBarVisibility(true, requireActivity().window)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(FOLDERS_SCREEN, "view created in FoldersFragment")
        super.onViewCreated(view, savedInstanceState)
        App.instance.appComponent.inject(this)
    }

    override fun init() {
        Log.d(FOLDERS_SCREEN, "initializing FoldersFragment")
        (activity as MainActivity).setSelectedTab(0)
        (activity as MainActivity).setToolbarVisibility(false)
        foldersTreeAdapter = FoldersTreeAdapter(presenter.foldersTreePresenter)
        binding.rvRoots.layoutManager = LinearLayoutManager(context)
        binding.rvRoots.adapter = foldersTreeAdapter

        initResultListeners()

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            presenter.onBackClick()
        }

        binding.fabAddRoots.setOnClickListener {
            presenter.onAddRootBtnClick()
        }
    }

    override fun setProgressVisibility(isVisible: Boolean, withText: String) {
        binding.layoutProgress.apply {
            root.isVisible = isVisible
            (activity as MainActivity).setBottomNavigationEnabled(!isVisible)

            if (withText.isNotEmpty()) {
                progressText.setVisibilityAndLoadingStatus(View.VISIBLE)
                progressText.loadingText = withText
            } else progressText.setVisibilityAndLoadingStatus(View.GONE)
        }
    }

    override fun updateFoldersTree() {
        foldersTreeAdapter?.dispatchUpdates()
    }

    override fun openRootPickerDialog(paths: List<Path>) {
        val dialog = FolderPickerDialogFragment.newInstance(paths)
        dialog.show(childFragmentManager, null)
    }

    override fun notifyUser(message: String, moreTime: Boolean) {
        Notifications.notifyUser(context, message, moreTime)
    }

    private fun initResultListeners() {
        childFragmentManager.setFragmentResultListener(
            FolderPickerDialogFragment.REQUEST_PATH_PICKED_KEY,
            this
        ) { _, bundle ->
            val path = bundle.getString(FolderPickerDialogFragment.RESULT_PATH_KEY)!!
            val rootNotFavorite =
                bundle.getBoolean(
                    FolderPickerDialogFragment.RESULT_ROOT_NOT_FAVORITE_KEY
                )
            presenter.onPickRootBtnClick(Path(path), rootNotFavorite)
        }
    }
}
