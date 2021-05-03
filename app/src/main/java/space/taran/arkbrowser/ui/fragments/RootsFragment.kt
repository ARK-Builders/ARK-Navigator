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
import space.taran.arkbrowser.mvp.model.entity.common.Icon
import space.taran.arkbrowser.mvp.model.entity.common.IconOrImage
import space.taran.arkbrowser.mvp.presenter.adapter.IItemGridPresenter
import space.taran.arkbrowser.mvp.presenter.adapter.ReversibleItemGridPresenter
import space.taran.arkbrowser.mvp.view.item.FileItemView
import space.taran.arkbrowser.ui.activity.MainActivity.Companion.REQUEST_CODE_SD_CARD_URI
import space.taran.arkbrowser.utils.getExtSdCards
import java.lang.IllegalStateException
import java.nio.file.Files
import java.nio.file.Path

class RootsFragment: MvpAppCompatFragment(), RootView, BackButtonListener {

    companion object {
        fun newInstance() = RootsFragment()
    }

    @InjectPresenter
    lateinit var presenter: RootsPresenter

    @ProvidePresenter
    fun providePresenter() =
        RootsPresenter(getExtSdCards(context!!)).apply {
            Log.d("flow", "creating RootsPresenter in RootsFragment")
            App.instance.appComponent.inject(this)
        }

    var adapter: ItemGridRVAdapter? = null
    var dialogAdapter: ItemGridRVAdapter? = null
    var dialogView: View? = null
    var alertDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("flow", "creating view in RootsFragment")
        return inflater.inflate(R.layout.fragment_roots, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("flow", "view created in RootsFragment")
        super.onViewCreated(view, savedInstanceState)
        App.instance.appComponent.inject(this)
    }

    override fun onResume() {
        Log.d("flow", "resuming in RootsFragment")
        super.onResume()
        presenter.onViewResumed()
    }

    override fun init() {
        Log.d("flow", "[mock] loading roots in RootsFragment")

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
        adapter = ItemGridRVAdapter(presenter.rootGridPresenter as IItemGridPresenter<Any>) //todo
        (activity as MainActivity).setToolbarVisibility(false)
        rv_roots.adapter = adapter
        fab_roots.setOnClickListener {
            presenter.fabClicked()
        }
    }

    override fun updateRootAdapter() {
        Log.d("flow", "updating root adapter in RootsFragment")
        adapter?.notifyDataSetChanged()
    }

    override fun updateDialogAdapter() {
        Log.d("flow", "updating dialog adapter in RootsFragment")
        dialogAdapter?.notifyDataSetChanged()
    }

    override fun openChooserDialog(files: List<Path>, handler: (Path) -> Unit) {
        Log.d("flow", "opening chooser dialog in RootsFragment")
        dialogView = LayoutInflater.from(context!!)
            .inflate(R.layout.dialog_roots_new, null)

        if (dialogView != null) {
            val dialogView = dialogView!!

            dialogView.rv_roots_dialog.layoutManager = GridLayoutManager(context, 2)
            dialogAdapter = ItemGridRVAdapter(DialogItemGridPresenter(files, handler) as IItemGridPresenter<Any>) //todo
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
        if (alertDialog != null) {
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
        } else {
            throw IllegalStateException("Failed to build AlertDialog")
        }
    }

    override fun setDialogPath(path: String) {
        Log.d("flow", "setting dialog path to $path in RootsFragment")

        dialogView?.tv_roots_dialog_path!!.text = path
    }

    override fun requestSdCardUri() {
        Log.d("flow", "requesting sd card URI in RootsFragment")
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        activity!!.startActivityForResult(intent, REQUEST_CODE_SD_CARD_URI)
    }

    override fun closeChooserDialog() {
        Log.d("flow", "closing chooser dialog in RootsFragment")
        alertDialog?.dismiss()
    }

    override fun backClicked(): Boolean {
        Log.d("flow", "back clicked in RootsFragment")
        return presenter.backClicked()
    }

    //todo
//    private fun storageVersionDifferent(fileStorageVersion: Int, root: remove_Root) {
//        viewState.showToast("${root.storage.path} has a different version")
//    }

    inner class DialogItemGridPresenter(initialFiles: List<Path>, handler: (Path) -> Unit):
        ReversibleItemGridPresenter<Path>(initialFiles, {
            Log.d("flow", "item $it clicked in RootsPresenter/DialogItemGridPresenter")

            if (Files.isDirectory(it)) {
                handler(it)
            } else {
                //todo: unclicable item
            }
        }) {

        override fun bindView(view: FileItemView) {
            Log.d("flow", "binding view in RootsPresenter/DialogItemGridPresenter")

            val paths = items()
            val path = paths[view.pos]
            view.setText(path.fileName.toString())
            if (Files.isDirectory(path)) {
                view.setIcon(IconOrImage(icon = Icon.FOLDER))
            } else {
                view.setIcon(IconOrImage(icon = Icon.FILE))
                //todo
//                if (path.isImage())
//                    view.setIcon(Icon.IMAGE, path.file)
//                else
//                    view.setIcon(Icon.FILE, path.file)
            }
        }
    }
}