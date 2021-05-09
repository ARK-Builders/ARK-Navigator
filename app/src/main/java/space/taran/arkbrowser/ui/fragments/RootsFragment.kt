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
import space.taran.arkbrowser.utils.ROOTS_FRAGMENT
import space.taran.arkbrowser.utils.listDevices
import java.lang.IllegalStateException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.toList

class RootsFragment: MvpAppCompatFragment(), RootView, BackButtonListener {

    companion object {
        fun newInstance() = RootsFragment() //todo ?

        const val PATH_PICKER_MESSAGE = "Add new root to the index"
    }

    @InjectPresenter
    lateinit var presenter: RootsPresenter

    @ProvidePresenter
    fun providePresenter() =
        RootsPresenter(listDevices(context!!)).apply {
            Log.d(ROOTS_FRAGMENT, "creating RootsPresenter in RootsFragment")
            App.instance.appComponent.inject(this)
        }

    private var adapter: ItemGridRVAdapter<Path>? = null
    private var dialogAdapter: ItemGridRVAdapter<Path>? = null
    private var dialogView: View? = null
    private var alertDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(ROOTS_FRAGMENT, "creating view in RootsFragment")
        return inflater.inflate(R.layout.fragment_roots, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(ROOTS_FRAGMENT, "view created in RootsFragment")
        super.onViewCreated(view, savedInstanceState)
        App.instance.appComponent.inject(this)
    }

    override fun onResume() {
        Log.d(ROOTS_FRAGMENT, "resuming in RootsFragment")
        super.onResume()
        presenter.onViewResumed()
    }

    override fun init() {
        Log.d(ROOTS_FRAGMENT, "[mock] loading roots in RootsFragment")

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

    override fun updateRootAdapter() {
        Log.d(ROOTS_FRAGMENT, "updating root adapter in RootsFragment")
        adapter?.notifyDataSetChanged()
    }

    override fun updateDialogAdapter() {
        Log.d(ROOTS_FRAGMENT, "updating dialog adapter in RootsFragment")
        dialogAdapter?.notifyDataSetChanged()
    }

    fun onClickHandler(): (Path) -> Unit = { path: Path ->
        Log.d("picker","Path $path was clicked")

        if (Files.isDirectory(path)) {
            val children = Files.list(path).toList()
            dialogAdapter!!.updateItems(children)
        } else {
            Log.d("picker","but it is not a directory")
        }
    }

    override fun openChooserDialog(paths: List<Path>, onFinish: (Path) -> Unit) {
        Log.d(ROOTS_FRAGMENT, "opening chooser dialog in RootsFragment")
        dialogView = LayoutInflater.from(context!!)
            .inflate(R.layout.dialog_roots_new, null)

        if (dialogView != null) {
            val dialogView = dialogView!!

            dialogView.tv_roots_dialog_path.text = PATH_PICKER_MESSAGE

            dialogView.rv_roots_dialog.layoutManager = GridLayoutManager(context, 2)

            dialogAdapter = ItemGridRVAdapter(
                RootPicker(
                    paths,
                    onClickHandler()
                )
            )
            dialogView.rv_roots_dialog.adapter = dialogAdapter

            dialogView.btn_roots_dialog_cancel.setOnClickListener {
                Log.d("ui", "[cancel] clicked")
                presenter.dismissDialog()
            }
            dialogView.btn_roots_dialog_pick.setOnClickListener {
                Log.d("ui", "[pick] clicked")
                presenter.rootPicked()
            }
        } else {
            throw IllegalStateException("Failed to inflate roots dialog View")
        }

        val alertDialogBuilder = AlertDialog.Builder(context!!)
            .setView(dialogView)

        alertDialog = alertDialogBuilder.show()
        if (alertDialog == null) {
            throw IllegalStateException("Failed to build AlertDialog")
        }
        val alertDialog = alertDialog!!

        alertDialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK &&
                event.action == KeyEvent.ACTION_UP &&
                !event.isCanceled
            ) {
                //todo
                dialogAdapter!!.backClicked()
                //presenter.backClicked()
                return@setOnKeyListener true
            }
            false
        }

        alertDialog.setCanceledOnTouchOutside(false)
    }

    override fun setDialogPath(path: String) {
        Log.d(ROOTS_FRAGMENT, "setting dialog path to $path in RootsFragment")

        dialogView?.tv_roots_dialog_path!!.text = path
    }

    override fun requestSdCardUri() {
        Log.d(ROOTS_FRAGMENT, "requesting sd card URI in RootsFragment")
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        activity!!.startActivityForResult(intent, REQUEST_CODE_SD_CARD_URI)
    }

    override fun closeChooserDialog() {
        Log.d(ROOTS_FRAGMENT, "closing chooser dialog in RootsFragment")
        alertDialog?.dismiss()
    }

    override fun backClicked(): Boolean {
        Log.d(ROOTS_FRAGMENT, "back clicked in RootsFragment")
        return presenter.backClicked()
    }

    //todo
//    private fun storageVersionDifferent(fileStorageVersion: Int, root: remove_Root) {
//        viewState.showToast("${root.storage.path} has a different version")
//    }
}