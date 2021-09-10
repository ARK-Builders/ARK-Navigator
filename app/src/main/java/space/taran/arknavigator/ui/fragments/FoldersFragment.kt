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
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
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
import space.taran.arknavigator.utils.listDevices
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject

class FoldersFragment: MvpAppCompatFragment(), FoldersView, BackButtonListener {
    private lateinit var devices: List<Path>

    private lateinit var foldersTree: FoldersTree
    private lateinit var folderPicker: FolderPicker

    private lateinit var roots: Set<Path>
    private lateinit var favorites: Set<Path>

    private lateinit var binding: FragmentFoldersBinding

    @Inject
    lateinit var router: Router

    private val presenter by moxyPresenter {
        FoldersPresenter().apply {
            Log.d(FOLDERS_SCREEN, "RootsPresenter created")
            App.instance.appComponent.inject(this)
        }
    }

    override fun loadFolders(folders: Folders) {
        Log.d(FOLDERS_SCREEN, "loading roots in FoldersFragment")

        val handler = { path: Path ->
            openFolderPicker(listOf(path))
        }

        foldersTree = FoldersTree(devices, folders, handler, router)
        binding.rvRoots.adapter = foldersTree

        roots = folders.keys
        favorites = folders.values.flatten().toSet()
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
        initialize()
    }

    override fun onResume() {
        Log.d(FOLDERS_SCREEN, "resuming in FoldersFragment")
        super.onResume()
        presenter.resume()
    }

    override fun backClicked(): Boolean {
        Log.d(FOLDERS_SCREEN, "[back] clicked in FoldersFragment")
        return presenter.quit()
    }

    override fun notifyUser(message: String, moreTime: Boolean) {
        Notifications.notifyUser(context, message, moreTime)
    }


    private fun initialize() {
        Log.d(FOLDERS_SCREEN, "initializing FoldersFragment")

        devices = listDevices(requireContext())

        (activity as MainActivity).setSelectedTab(0)
        (activity as MainActivity).setToolbarVisibility(false)

        binding.fabAddRoots.setOnClickListener {
            openFolderPicker(devices)
        }
    }

    private fun openFolderPicker(paths: List<Path>) {
        Log.d(FOLDERS_SCREEN, "initializing root picker")

        val dialogBinding = DialogRootsNewBinding.inflate(
            LayoutInflater.from(requireContext()))

        dialogBinding.rvRootsDialog.layoutManager = GridLayoutManager(context, 2)
        folderPicker = FolderPicker(paths, rootPickerClickHandler(dialogBinding), dialogBinding)

        var alertDialog: AlertDialog? = null

        dialogBinding.btnRootsDialogCancel.setOnClickListener {
            Log.d(FOLDER_PICKER, "[cancel] pressed, closing root picker")
            alertDialog?.dismiss()
        }
        dialogBinding.btnRootsDialogPick.setOnClickListener {
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

        alertDialog = rootPickerAlertDialog(dialogBinding.root)
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
    private fun rootPickerClickHandler(dialogBinding: DialogRootsNewBinding): ItemClickHandler<Path> = { _, path: Path ->
        Log.d(FOLDER_PICKER,"path $path was clicked")

        dialogBinding.apply {
            if (Files.isDirectory(path)) {
                folderPicker.updatePath(path)

                val rootPrefix = roots.find { path.startsWith(it) }
                if (rootPrefix != null) {
                    if (rootPrefix == path) {
                        btnRootsDialogPick.isEnabled = false
                        btnRootsDialogPick.text = PICK_ROOT
                        rootNotFavorite = true
                    } else {
                        btnRootsDialogPick.isEnabled = true
                        btnRootsDialogPick.text = PICK_FAVORITE
                        rootNotFavorite = false
                    }
                } else {
                    btnRootsDialogPick.isEnabled = true
                    btnRootsDialogPick.text = PICK_ROOT
                    rootNotFavorite = true
                }
            } else {
                Log.d(FOLDER_PICKER,"but it is not a directory")
                notifyUser(FILE_CHOSEN_AS_ROOT)
            }
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