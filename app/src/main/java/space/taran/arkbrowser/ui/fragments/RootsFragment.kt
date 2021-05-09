package space.taran.arkbrowser.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import space.taran.arkbrowser.R
import space.taran.arkbrowser.mvp.presenter.RootsPresenter
import space.taran.arkbrowser.mvp.view.RootView
import space.taran.arkbrowser.ui.App
import space.taran.arkbrowser.ui.activity.MainActivity
import space.taran.arkbrowser.ui.adapter.ItemGridRVAdapter
import kotlinx.android.synthetic.main.dialog_roots_new.view.*
import kotlinx.android.synthetic.main.fragment_roots.*
import moxy.MvpAppCompatFragment
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import space.taran.arkbrowser.mvp.presenter.utils.RootPicker
import space.taran.arkbrowser.ui.activity.MainActivity.Companion.REQUEST_CODE_SD_CARD_URI
import space.taran.arkbrowser.utils.ROOTS_SCREEN
import space.taran.arkbrowser.utils.ROOT_PICKER
import space.taran.arkbrowser.utils.listDevices
import java.lang.IllegalStateException
import java.nio.file.Files
import java.nio.file.Path

class RootsFragment: MvpAppCompatFragment(), RootView, BackButtonListener {

    companion object {
        fun newInstance() = RootsFragment() //todo ?
    }

    @InjectPresenter
    lateinit var presenter: RootsPresenter

    @ProvidePresenter
    fun providePresenter() =
        RootsPresenter(listDevices(context!!)).apply {
            Log.d(ROOTS_SCREEN, "creating RootsPresenter in RootsFragment")
            App.instance.appComponent.inject(this)
        }

    private var adapter: ItemGridRVAdapter<Unit, Path>? = null
    private var rootPicker: RootPicker? = null

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
    }

    override fun onResume() {
        Log.d(ROOTS_SCREEN, "resuming in RootsFragment")
        super.onResume()
        presenter.onViewResumed()
    }

    override fun init() {
        Log.d(ROOTS_SCREEN, "[mock] loading roots in RootsFragment")

//        roomRepo.getAllRoots().observeOn(AndroidSchedulers.mainThread()).subscribe(
//            { list ->
//                list.forEach { root ->
//                    val storageVersion = resourcesRepo.readStorageVersion(root.storage)
//                    if (storageVersion != ResourcesRepo.STORAGE_VERSION)
//                        storageVersionDifferent(storageVersion, root)
//                    rootsRepo.synchronizeRoot(root)
//                }
//            },
//            {}
//        )

        (activity as MainActivity).setSelectedTab(0)
        rv_roots.layoutManager = GridLayoutManager(context, 3)
        adapter = ItemGridRVAdapter(presenter.rootGridPresenter) //todo
        (activity as MainActivity).setToolbarVisibility(false)
        rv_roots.adapter = adapter
        fab_roots.setOnClickListener {
            presenter.fabClicked()
        }
    }

    override fun updateAdapter() {
        Log.d(ROOTS_SCREEN, "updating root adapter in RootsFragment")
        adapter?.notifyDataSetChanged()
    }

    fun onClickHandler(): (Path) -> Unit = { path: Path ->
        Log.d(ROOT_PICKER,"path $path was clicked")

        if (Files.isDirectory(path)) {
            rootPicker!!.updatePath(path)
        } else {
            Log.d(ROOT_PICKER,"but it is not a directory")
        }
    }

    override fun openRootPicker(paths: List<Path>, onFinish: (Path) -> Unit) {
        Log.d(ROOTS_SCREEN, "opening chooser dialog in RootsFragment")
        val dialogView_ = LayoutInflater.from(context!!)
            .inflate(R.layout.dialog_roots_new, null)

        var alertDialog_: AlertDialog? = null

        if (dialogView_ != null) {
            val dialogView = dialogView_!!

            dialogView.rv_roots_dialog.layoutManager = GridLayoutManager(context, 2)

            rootPicker = RootPicker(paths, onClickHandler(), dialogView)

            dialogView.btn_roots_dialog_cancel.setOnClickListener {
                Log.d(ROOT_PICKER, "[cancel] pressed, closing root picker")
                alertDialog_?.dismiss()
            }
            dialogView.btn_roots_dialog_pick.setOnClickListener {
                Log.d("ui", "[pick] clicked")
                presenter.rootPicked()
            }
        } else {
            throw IllegalStateException("Failed to inflate roots dialog View")
        }

        val alertDialogBuilder = AlertDialog.Builder(context!!)
            .setView(dialogView_)

        alertDialog_ = alertDialogBuilder.show()
        if (alertDialog_ == null) {
            throw IllegalStateException("Failed to build AlertDialog")
        }

        val alertDialog = alertDialog_!!

        alertDialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK &&
                event.action == KeyEvent.ACTION_UP &&
                !event.isCanceled
            ) {
                Log.d(ROOT_PICKER, "[back] pressed")
                if (rootPicker!!.backClicked() == null) {
                    Log.d(ROOT_PICKER, "can't go back, closing root picker")
                    alertDialog.dismiss()
                }
                return@setOnKeyListener true
            }
            false
        }

        alertDialog.setCanceledOnTouchOutside(false)
    }

    override fun requestSdCardUri() {
        Log.d(ROOTS_SCREEN, "requesting sd card URI in RootsFragment")
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        activity!!.startActivityForResult(intent, REQUEST_CODE_SD_CARD_URI)
    }

    override fun backClicked(): Boolean {
        Log.d(ROOTS_SCREEN, "back clicked in RootsFragment")
        return presenter.backClicked()
    }

    //todo
//    private fun storageVersionDifferent(fileStorageVersion: Int, root: remove_Root) {
//        viewState.showToast("${root.storage.path} has a different version")
//    }
}