package space.taran.arkbrowser.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.dialog_roots_new.view.*
import kotlinx.android.synthetic.main.fragment_roots.*
import moxy.MvpAppCompatFragment
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import space.taran.arkbrowser.R
import space.taran.arkbrowser.mvp.model.repo.Folders
import space.taran.arkbrowser.mvp.presenter.RootsPresenter
import space.taran.arkbrowser.mvp.presenter.utils.FoldersTree
import space.taran.arkbrowser.mvp.presenter.utils.RootPicker
import space.taran.arkbrowser.mvp.view.RootView
import space.taran.arkbrowser.ui.App
import space.taran.arkbrowser.ui.activity.MainActivity
import space.taran.arkbrowser.utils.ROOTS_SCREEN
import space.taran.arkbrowser.utils.ROOT_PICKER
import space.taran.arkbrowser.utils.listDevices
import java.nio.file.Files
import java.nio.file.Path

class RootsFragment: MvpAppCompatFragment(), RootView, BackButtonListener {
    private lateinit var devices: List<Path>

    private lateinit var foldersTree: FoldersTree
    private lateinit var rootPicker: RootPicker

    private lateinit var roots: Set<Path>

    //private lateinit var favorites: Set<Path>
    //todo: use it for hybrid picker (when we descend into already chosen root,
    // we start choosing favorite folder since we can't have nested roots)

    @InjectPresenter
    lateinit var presenter: RootsPresenter

    @ProvidePresenter
    fun providePresenter() =
        RootsPresenter().apply {
            Log.d(ROOTS_SCREEN, "RootsPresenter created")
            App.instance.appComponent.inject(this)
        }


    override fun loadFolders(folders: Folders) {
        Log.d(ROOTS_SCREEN, "loading roots in RootsFragment")

        foldersTree = FoldersTree(devices, folders)
        rv_roots.adapter = foldersTree

        roots = folders.keys
        //favorites = folders.values.flatten().toSet()
        //todo
    }

    override fun notifyUser(message: String, moreTime: Boolean) {
        val duration = if (moreTime) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        Toast.makeText(context, message, duration).show()
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(ROOTS_SCREEN, "creating view in RootsFragment")
        return inflater.inflate(R.layout.fragment_roots, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(ROOTS_SCREEN, "view created in RootsFragment")
        super.onViewCreated(view, savedInstanceState)
        App.instance.appComponent.inject(this)
        initialize()
    }

    override fun onResume() {
        Log.d(ROOTS_SCREEN, "resuming in RootsFragment")
        super.onResume()
        presenter.resume()
    }

    override fun backClicked(): Boolean {
        Log.d(ROOTS_SCREEN, "back clicked in RootsFragment")
        return presenter.quit()
    }


    private fun initialize() {
        Log.d(ROOTS_SCREEN, "initializing RootsFragment")

        devices = listDevices(requireContext())

        (activity as MainActivity).setSelectedTab(0)
        (activity as MainActivity).setToolbarVisibility(false)

        fab_add_roots.setOnClickListener {
            openRootPicker(devices)
        }
    }

    private fun openRootPicker(paths: List<Path>) {
        Log.d(ROOTS_SCREEN, "initializing root picker")

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_roots_new, null)
            ?: throw IllegalStateException("Failed to inflate dialog View for roots picker")

        dialogView.rv_roots_dialog.layoutManager = GridLayoutManager(context, 2)
        rootPicker = RootPicker(paths, rootPickerClickHandler(), dialogView)

        var alertDialog: AlertDialog? = null

        dialogView.btn_roots_dialog_cancel.setOnClickListener {
            Log.d(ROOT_PICKER, "[cancel] pressed, closing root picker")
            alertDialog?.dismiss()
        }
        dialogView.btn_roots_dialog_pick.setOnClickListener {
            Log.d(ROOT_PICKER, "[pick] pressed")

            val path = rootPicker.getLabel()
            if (!devices.contains(path)) {
                if (roots.contains(path)) {
                    notifyUser(ROOT_IS_ALREADY_PICKED)
                } else {
                    presenter.addRoot(path)
                    alertDialog?.dismiss()
                }
            } else {
                Log.d(ROOT_PICKER,"potentially huge directory")
                notifyUser(DEVICE_CHOSEN_AS_ROOT)
            }
        }

        alertDialog = rootPickerAlertDialog(dialogView)
        Log.d(ROOTS_SCREEN, "root picker initialized")
    }

    private fun rootPickerAlertDialog(view: View): AlertDialog {
        val builder = AlertDialog.Builder(requireContext()).setView(view)

        val result = builder.show()
            ?: throw IllegalStateException("Failed to create AlertDialog")

        result.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK &&
                event.action == KeyEvent.ACTION_UP &&
                !event.isCanceled) {

                Log.d(ROOT_PICKER, "[back] pressed")
                if (rootPicker.backClicked() == null) {
                    Log.d(ROOT_PICKER, "can't go back, closing root picker")
                    result.dismiss()
                }
                return@setOnKeyListener true
            }
            false
        }

        result.setCanceledOnTouchOutside(false)
        return result
    }

    private fun rootPickerClickHandler(): (Path) -> Unit = { path: Path ->
        Log.d(ROOT_PICKER,"path $path was clicked")

        if (Files.isDirectory(path)) {
            rootPicker.updatePath(path)
        } else {
            Log.d(ROOT_PICKER,"but it is not a directory")
            notifyUser(FILE_CHOSEN_AS_ROOT)
        }
    }

    companion object {
        private const val DEVICE_CHOSEN_AS_ROOT =
            "Huge directories can cause long waiting times"
        private const val ROOT_IS_ALREADY_PICKED =
            "This folder is already picked as root"
        private const val FILE_CHOSEN_AS_ROOT =
            "Can't go inside a file"
    }
}