package space.taran.arkbrowser.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.dialog_roots_new.view.*
import kotlinx.android.synthetic.main.fragment_roots.*
import moxy.MvpAppCompatFragment
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import ru.terrakok.cicerone.Router
import space.taran.arkbrowser.R
import space.taran.arkbrowser.mvp.model.repo.Folders
import space.taran.arkbrowser.mvp.presenter.FoldersPresenter
import space.taran.arkbrowser.mvp.presenter.FoldersTree
import space.taran.arkbrowser.mvp.presenter.FolderPicker
import space.taran.arkbrowser.mvp.view.FoldersView
import space.taran.arkbrowser.ui.App
import space.taran.arkbrowser.ui.activity.MainActivity
import space.taran.arkbrowser.ui.fragments.utils.Notifications
import space.taran.arkbrowser.utils.FOLDERS_SCREEN
import space.taran.arkbrowser.utils.FOLDER_PICKER
import space.taran.arkbrowser.utils.listDevices
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject

class FoldersFragment: MvpAppCompatFragment(), FoldersView, BackButtonListener {
    private lateinit var devices: List<Path>
    //todo treat syncthing folder as special storage device

    private lateinit var foldersTree: FoldersTree
    private lateinit var folderPicker: FolderPicker

    private lateinit var roots: Set<Path>
    private lateinit var favorites: Set<Path>

    @Inject
    lateinit var router: Router

    @InjectPresenter
    lateinit var presenter: FoldersPresenter

    @ProvidePresenter
    fun providePresenter() =
        FoldersPresenter().apply {
            Log.d(FOLDERS_SCREEN, "RootsPresenter created")
            App.instance.appComponent.inject(this)
        }


    override fun loadFolders(folders: Folders) {
        Log.d(FOLDERS_SCREEN, "loading roots in RootsFragment")

        foldersTree = FoldersTree(devices, folders, router)
        rv_roots.adapter = foldersTree

        roots = folders.keys
        favorites = folders.values.flatten().toSet()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(FOLDERS_SCREEN, "creating view in RootsFragment")
        return inflater.inflate(R.layout.fragment_roots, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(FOLDERS_SCREEN, "view created in RootsFragment")
        super.onViewCreated(view, savedInstanceState)
        App.instance.appComponent.inject(this)
        initialize()
    }

    override fun onResume() {
        Log.d(FOLDERS_SCREEN, "resuming in RootsFragment")
        super.onResume()
        presenter.resume()
    }

    override fun backClicked(): Boolean {
        Log.d(FOLDERS_SCREEN, "back clicked in RootsFragment")
        return presenter.quit()
    }

    override fun notifyUser(message: String, moreTime: Boolean) {
        Notifications.notifyUser(context, message, moreTime)
    }


    private fun initialize() {
        Log.d(FOLDERS_SCREEN, "initializing RootsFragment")

        devices = listDevices(requireContext())

        (activity as MainActivity).setSelectedTab(0)
        (activity as MainActivity).setToolbarVisibility(false)

        fab_add_roots.setOnClickListener {
            openRootPicker(devices)
        }
    }

    private fun openRootPicker(paths: List<Path>) {
        Log.d(FOLDERS_SCREEN, "initializing root picker")

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_roots_new, null)
            ?: throw IllegalStateException("Failed to inflate dialog View for roots picker")

        dialogView.rv_roots_dialog.layoutManager = GridLayoutManager(context, 2)
        folderPicker = FolderPicker(paths, rootPickerClickHandler(dialogView), dialogView)

        var alertDialog: AlertDialog? = null

        dialogView.btn_roots_dialog_cancel.setOnClickListener {
            Log.d(FOLDER_PICKER, "[cancel] pressed, closing root picker")
            alertDialog?.dismiss()
        }
        dialogView.btn_roots_dialog_pick.setOnClickListener {
            Log.d(FOLDER_PICKER, "[pick] pressed")

            val path = folderPicker.getLabel()
            if (!devices.contains(path)) {
                if (rootNotFavorite) {
                    // adding path as root
                    if (roots.contains(path)) {
                        notifyUser(ROOT_IS_ALREADY_PICKED)
                    } else {
                        presenter.addRoot(path)
                        alertDialog?.dismiss()
                    }
                } else {
                    // adding path as favorite
                    if (favorites.contains(path)) {
                        notifyUser(FAVORITE_IS_ALREADY_PICKED)
                    } else {
                        presenter.addFavorite(path)
                        alertDialog?.dismiss()
                    }
                }
            } else {
                Log.d(FOLDER_PICKER,"potentially huge directory")
                notifyUser(DEVICE_CHOSEN_AS_ROOT)
            }
        }

        alertDialog = rootPickerAlertDialog(dialogView)
        Log.d(FOLDERS_SCREEN, "root picker initialized")
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
                if (folderPicker.backClicked() == null) {
                    Log.d(FOLDER_PICKER, "can't go back, closing root picker")
                    result.dismiss()
                }
                return@setOnKeyListener true
            }
            false
        }

        result.setCanceledOnTouchOutside(false)
        return result
    }

    //todo don't pass dialogView here, draw it from new "model"
    private fun rootPickerClickHandler(dialogView: View): (Path) -> Unit = { path: Path ->
        Log.d(FOLDER_PICKER,"path $path was clicked")

        if (Files.isDirectory(path)) {
            folderPicker.updatePath(path)

            val rootPrefix = roots.find { path.startsWith(it) }
            if (rootPrefix != null) {
                if (rootPrefix == path) {
                    //todo fake disabling (still show messages when pressing on disabled button)
                    //todo consistent rules for onPick messages and gray-out
                    //todo revert button state when backClicked
                    dialogView.btn_roots_dialog_pick.isEnabled = false
                    dialogView.btn_roots_dialog_pick.text = PICK_ROOT
                    rootNotFavorite = true
                } else {
                    dialogView.btn_roots_dialog_pick.isEnabled = true
                    dialogView.btn_roots_dialog_pick.text = PICK_FAVORITE
                    rootNotFavorite = false
                }
            } else {
                dialogView.btn_roots_dialog_pick.isEnabled = true
                dialogView.btn_roots_dialog_pick.text = PICK_ROOT
                rootNotFavorite = true
            }
        } else {
            Log.d(FOLDER_PICKER,"but it is not a directory")
            notifyUser(FILE_CHOSEN_AS_ROOT)
        }
    }

    //todo move it somewhere
    private var rootNotFavorite: Boolean = true

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