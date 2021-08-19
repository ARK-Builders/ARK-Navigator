package space.taran.arknavigator.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import moxy.MvpAppCompatFragment
import moxy.ktx.moxyPresenter
import ru.terrakok.cicerone.Router
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.DialogRootsNewBinding
import space.taran.arknavigator.databinding.FragmentFoldersBinding
import space.taran.arknavigator.mvp.model.repo.Folders
import space.taran.arknavigator.mvp.presenter.FoldersPresenter
import space.taran.arknavigator.ui.adapter.FoldersTree
import space.taran.arknavigator.ui.adapter.FolderPicker
import space.taran.arknavigator.mvp.presenter.adapter.ItemClickHandler
import space.taran.arknavigator.mvp.view.FoldersView
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.ui.activity.MainActivity
import space.taran.arknavigator.ui.fragments.utils.Notifications
import space.taran.arknavigator.utils.FOLDERS_SCREEN
import space.taran.arknavigator.utils.FOLDER_PICKER
import java.nio.file.Path
import javax.inject.Inject

class FoldersFragment: MvpAppCompatFragment(), FoldersView, BackButtonListener {
    @Inject
    lateinit var router: Router

    private lateinit var foldersTree: FoldersTree
    private var folderPicker: FolderPicker? = null

    private var rootPickerDialogView: View? = null
    private var rootPickerDialog: AlertDialog? = null

    private lateinit var binding: FragmentFoldersBinding
    private val presenter by moxyPresenter {
        FoldersPresenter().apply {
            Log.d(FOLDERS_SCREEN, "RootsPresenter created")
            App.instance.appComponent.inject(this)
        }
    }

    override fun setProgressVisibility(isVisible: Boolean) {
        binding.layoutProgress.root.isVisible = isVisible
        (activity as MainActivity).setBottomNavigationEnabled(!isVisible)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        Log.d(FOLDERS_SCREEN, "inflating layout for FoldersFragment")
        binding = FragmentFoldersBinding.inflate(inflater, container, false)
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

        fab_add_roots.setOnClickListener {
            presenter.onAddRootBtnClick()
        }
    }

    override fun updateFoldersTree(devices: List<Path>, folders: Folders) {
        foldersTree = FoldersTree(devices, folders, presenter::onFoldersTreeAddFavoriteBtnClick, router)
        rv_roots.adapter = foldersTree
    }

    override fun updateRootPickerDialogPath(path: Path) {
        folderPicker?.updatePath(path)
    }

    //todo fake disabling (still show messages when pressing on disabled button)
    //todo consistent rules for onPick messages and gray-out
    //todo revert button state when backClicked
    override fun updateRootPickerDialogPickBtnState(isEnabled: Boolean, isRoot: Boolean) {
        rootPickerDialogView?.btn_roots_dialog_pick?.isEnabled = isEnabled
        if (isRoot)
            rootPickerDialogView?.btn_roots_dialog_pick?.text = PICK_ROOT
        else
            rootPickerDialogView?.btn_roots_dialog_pick?.text = PICK_FAVORITE
    }

    //provide null to close the dialog
    override fun setRootPickerDialogVisibility(paths: List<Path>?) {
        if (paths == null) {
            rootPickerDialog?.dismiss()
            return
        }

        Log.d(FOLDERS_SCREEN, "initializing root picker")

        rootPickerDialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_roots_new, null)
            ?: throw IllegalStateException("Failed to inflate dialog View for roots picker")

        rootPickerDialogView!!.rv_roots_dialog.layoutManager = GridLayoutManager(context, 2)
        folderPicker = FolderPicker(paths, presenter.onRootPickerItemClick(), rootPickerDialogView!!)

        rootPickerDialogView!!.btn_roots_dialog_cancel.setOnClickListener {
            Log.d(FOLDER_PICKER, "[cancel] pressed, closing root picker")
            presenter.onRootPickerCancelClick()
        }
        rootPickerDialogView!!.btn_roots_dialog_pick.setOnClickListener {
            Log.d(FOLDER_PICKER, "[pick] pressed")
            presenter.onPickRootBtnClick(folderPicker!!.getLabel())
        }

        rootPickerDialog = rootPickerAlertDialog(rootPickerDialogView!!)
        Log.d(FOLDERS_SCREEN, "root picker initialized")
    }

    override fun notifyUser(message: String, moreTime: Boolean) {
        Notifications.notifyUser(context, message, moreTime)
    }

    override fun backClicked(): Boolean {
        Log.d(FOLDERS_SCREEN, "[back] clicked in FoldersFragment")
        return presenter.onBackClick()
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
                }
                return@setOnKeyListener true
            }
            false
        }

        result.setCanceledOnTouchOutside(false)
        return result
    }
    companion object {
        private const val DEVICE_CHOSEN_AS_ROOT =
            "Huge directories can cause long waiting times"
        private const val FAVORITE_IS_ALREADY_PICKED =
            "This folder is already among your favorites"
        private const val ROOT_IS_ALREADY_PICKED =
            "This folder is already picked as root"
        private const val FILE_CHOSEN_AS_ROOT =
            "Can't go inside a file"

        private const val PICK_FAVORITE =
            "FAVORITE"
        private const val PICK_ROOT =
            "ADD ROOT"
    }
}