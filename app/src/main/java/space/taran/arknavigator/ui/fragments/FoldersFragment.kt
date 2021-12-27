package space.taran.arknavigator.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import moxy.MvpAppCompatFragment
import moxy.ktx.moxyPresenter
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.DialogRootsNewBinding
import space.taran.arknavigator.databinding.FragmentFoldersBinding
import space.taran.arknavigator.mvp.presenter.FoldersPresenter
import space.taran.arknavigator.mvp.view.FoldersView
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.ui.activity.MainActivity
import space.taran.arknavigator.ui.adapter.FolderPicker
import space.taran.arknavigator.ui.adapter.folderstree.FoldersTreeAdapter
import space.taran.arknavigator.ui.fragments.utils.Notifications
import space.taran.arknavigator.utils.FOLDERS_SCREEN
import space.taran.arknavigator.utils.FOLDER_PICKER
import space.taran.arknavigator.utils.FullscreenHelper
import java.nio.file.Path

class FoldersFragment: MvpAppCompatFragment(), FoldersView {
    private var foldersTreeAdapter: FoldersTreeAdapter? = null

    private var folderPicker: FolderPicker? = null
    private var rootPickerDialog: AlertDialog? = null
    private var rootPickerBinding: DialogRootsNewBinding? = null

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
        savedInstanceState: Bundle?): View {
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
            }
            else progressText.setVisibilityAndLoadingStatus(View.GONE)
        }
    }

    override fun updateFoldersTree() {
        foldersTreeAdapter?.dispatchUpdates()
    }

    override fun updateRootPickerDialogPath(path: Path) {
        folderPicker?.updatePath(path)
    }

    override fun updateRootPickerDialogPickBtnState(isEnabled: Boolean, isRoot: Boolean) {
        rootPickerBinding?.btnRootsDialogPick?.isEnabled = isEnabled
        if (isRoot)
            rootPickerBinding?.btnRootsDialogPick?.text = requireContext().getString(R.string.folders_pick_root)
        else
            rootPickerBinding?.btnRootsDialogPick?.text = requireContext().getString(R.string.folders_pick_favorite)
    }

    override fun openRootPickerDialog(paths: List<Path>) {
        Log.d(FOLDERS_SCREEN, "initializing root picker")

        rootPickerBinding = DialogRootsNewBinding.inflate(
            LayoutInflater.from(requireContext()))

        rootPickerBinding!!.rvRootsDialog.layoutManager = GridLayoutManager(context, 2)
        folderPicker = FolderPicker(paths, presenter.onRootPickerItemClick(), rootPickerBinding!!)

        rootPickerBinding!!.btnRootsDialogCancel.setOnClickListener {
            Log.d(FOLDER_PICKER, "[cancel] pressed, closing root picker")
            presenter.onRootPickerCancelClick()
        }
        rootPickerBinding!!.btnRootsDialogPick.setOnClickListener {
            Log.d(FOLDER_PICKER, "[pick] pressed")
            presenter.onPickRootBtnClick(folderPicker!!.getLabel())
        }

        rootPickerDialog = rootPickerAlertDialog(rootPickerBinding!!.root)
        Log.d(FOLDERS_SCREEN, "root picker initialized")
    }

    override fun closeRootPickerDialog() {
        rootPickerDialog?.dismiss()
    }

    override fun notifyUser(message: String, moreTime: Boolean) {
        Notifications.notifyUser(context, message, moreTime)
    }

    private fun rootPickerAlertDialog(view: View): AlertDialog {
        val builder = AlertDialog.Builder(requireContext()).setView(view)

        val result = builder.show()
            ?: throw IllegalStateException("Failed to create AlertDialog")

        result.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK &&
                event.action == KeyEvent.ACTION_UP &&
                !event.isCanceled) {

                Log.d(FOLDER_PICKER, "[back] pressed")
                if (folderPicker!!.backClicked() == null) {
                    Log.d(FOLDER_PICKER, "can't go back, closing root picker")
                    presenter.onRootPickerBackClick()
                } else {
                    presenter.navigateBackClick(folderPicker!!.currentLabel)
                }
                return@setOnKeyListener true
            }
            false
        }

        result.setCanceledOnTouchOutside(false)
        return result
    }
}