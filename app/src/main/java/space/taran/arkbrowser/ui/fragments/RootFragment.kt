package space.taran.arkbrowser.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import space.taran.arkbrowser.R
import space.taran.arkbrowser.mvp.presenter.RootPresenter
import space.taran.arkbrowser.mvp.view.RootView
import space.taran.arkbrowser.ui.App
import space.taran.arkbrowser.ui.MainActivity
import space.taran.arkbrowser.ui.adapter.FileGridRVAdapter
import kotlinx.android.synthetic.main.dialog_roots_new.view.*
import kotlinx.android.synthetic.main.fragment_roots.*
import moxy.MvpAppCompatFragment
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter

class RootFragment: MvpAppCompatFragment(), RootView, BackButtonListener {

    companion object {
        fun newInstance() = RootFragment()
    }

    @InjectPresenter
    lateinit var presenter: RootPresenter

    @ProvidePresenter
    fun providePresenter() =
        RootPresenter().apply {
            App.instance.appComponent.inject(this)
        }

    var adapter: FileGridRVAdapter? = null
    var dialogAdapter: FileGridRVAdapter? = null
    var dialogView: View? = null
    var dialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_roots, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        App.instance.appComponent.inject(this)
    }

    override fun onResume() {
        super.onResume()
        presenter.onViewResumed()
    }

    override fun init() {
        rv_roots.layoutManager = GridLayoutManager(context, 3)
        adapter = FileGridRVAdapter(presenter.rootGridPresenter)
        (activity as MainActivity).setToolbarVisibility(false)
        rv_roots.adapter = adapter
        fab_roots.setOnClickListener {
            presenter.fabClicked()
        }
    }

    override fun updateRootAdapter() {
        adapter?.notifyDataSetChanged()
    }

    override fun updateDialogAdapter() {
        dialogAdapter?.notifyDataSetChanged()
    }

    override fun openChooserDialog() {
        dialogView = LayoutInflater.from(context!!).inflate(R.layout.dialog_roots_new, null)
        val alertDialogBuilder = AlertDialog.Builder(context!!).setView(dialogView)
        dialogView!!.rv_roots_dialog.layoutManager = GridLayoutManager(context, 2)
        dialogAdapter = FileGridRVAdapter(presenter.dialogGridPresenter)
        dialogView!!.rv_roots_dialog.adapter = dialogAdapter
        dialogView!!.btn_roots_dialog_cancel.setOnClickListener {
            presenter.dismissDialog()
        }

        dialogView!!.btn_roots_dialog_pick.setOnClickListener {
            presenter.rootPicked()
        }

        dialog = alertDialogBuilder.show()

        dialog!!.setOnKeyListener { dialog, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK &&
                event.action == KeyEvent.ACTION_UP &&
                !event.isCanceled
            ) {
                presenter.backClicked()
                return@setOnKeyListener true
            }
            false
        }

        dialog!!.setCanceledOnTouchOutside(false)
    }

    override fun setDialogPath(path: String) {
        dialogView?.tv_roots_dialog_path!!.text = path
    }

    override fun setSelectedTab(pos: Int) {
        (activity as MainActivity).setSelectedTab(pos)
    }

    override fun requestSdCardUri() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        activity!!.startActivityForResult(intent, 2)
    }

    override fun closeChooserDialog() {
        dialog?.dismiss()
    }

    override fun backClicked() = presenter.backClicked()
}