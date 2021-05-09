package space.taran.arkbrowser.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import space.taran.arkbrowser.R
import space.taran.arkbrowser.mvp.presenter.ExplorerPresenter
import space.taran.arkbrowser.mvp.view.ExplorerView
import space.taran.arkbrowser.ui.App
import space.taran.arkbrowser.ui.activity.MainActivity
import space.taran.arkbrowser.ui.adapter.ItemGridRVAdapter
import kotlinx.android.synthetic.main.fragment_explorer.*
import moxy.MvpAppCompatFragment
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import space.taran.arkbrowser.ui.activity.MainActivity.Companion.REQUEST_CODE_SD_CARD_URI
import space.taran.arkbrowser.utils.MarkableFile
import java.nio.file.Path
import java.nio.file.Paths

class ExplorerFragment : MvpAppCompatFragment(), ExplorerView, BackButtonListener {

    companion object {
        const val FOLDER_KEY = "file"

        fun newInstance(folder: Path?) = ExplorerFragment().apply {
            Log.d("flow", "[mock] creating ExplorerFragment")
            arguments = Bundle().apply {
                putString(FOLDER_KEY, folder.toString())
            }
        }
    }

    var dialog: AlertDialog? = null

    @InjectPresenter
    lateinit var presenter: ExplorerPresenter

    @ProvidePresenter
    fun providePresenter(): ExplorerPresenter {
        val arg = arguments!![FOLDER_KEY] as String?
        val path = if (arg != null) {
            Paths.get(arg)
        } else {
            null
        }

        return ExplorerPresenter(path).apply {
            Log.d("flow", "creating ExplorerPresenter in ExplorerFragment")
            App.instance.appComponent.inject(this)
        }
    }

    var adapter: ItemGridRVAdapter<Unit, MarkableFile>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("flow", "creating view in ExplorerFragment")
        return inflater.inflate(R.layout.fragment_explorer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("flow", "view created in ExplorerFragment")
        super.onViewCreated(view, savedInstanceState)
        App.instance.appComponent.inject(this)
    }

    override fun onResume() {
        Log.d("flow", "resuming in ExplorerFragment")
        super.onResume()
        presenter.onViewResumed()
    }

    override fun init() {
        Log.d("flow", "initializing ExplorerFragment")
        (activity as MainActivity).setSelectedTab(2)
        rv_files.layoutManager = GridLayoutManager(context, 3)
        adapter = ItemGridRVAdapter(presenter.fileGridPresenter!!)
        (activity as MainActivity).setToolbarVisibility(true)
        fab_explorer_fav.setOnClickListener {
            presenter.favFabClicked()
        }
        fab_explorer_tags.setOnClickListener {
            presenter.tagsFabClicked()
        }

        rv_files.adapter = adapter
    }

    override fun setFavoriteFabVisibility(isVisible: Boolean) {
        Log.d("flow", "setting favorites fab vsibility in ExplorerFragment")

        if (isVisible)
            fab_explorer_fav.visibility = View.VISIBLE
        else
            fab_explorer_fav.visibility = View.GONE
    }

    override fun setTagsFabVisibility(isVisible: Boolean) {
        Log.d("flow", "setting tags fab vsibility in ExplorerFragment")

        if (isVisible)
            fab_explorer_tags.visibility = View.VISIBLE
        else
            fab_explorer_tags.visibility = View.GONE
    }

    override fun openFile(uri: Uri, mimeType: String) {
        Log.d("flow", "opening file $uri in ExplorerFragment")
        val intent = Intent(Intent.ACTION_EDIT)
        intent.setDataAndType(uri, mimeType)
        startActivity(intent)
    }

    override fun showDialog() {
        dialog = MaterialAlertDialogBuilder(context!!)
            .setTitle("Do you want to add a folder to the home screen?")
            .setPositiveButton("OK") { _, _ ->
                presenter.favoriteChanged()
            }
            .setNegativeButton("Cancel") { _, _ ->
                presenter.dismissDialog()
            }
            .show()

        dialog!!.setOnCancelListener {
            presenter.dismissDialog()
        }
    }

    override fun setTitle(title: String) {
        (activity as MainActivity).setTitle(title)
    }

    override fun showToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    override fun closeDialog() {
        Log.d("flow", "closing dialog in ExplorerFragment")
        dialog?.dismiss()
    }

    override fun requestSdCardUri() {
        Log.d("flow", "requesting sdcarf uri in ExplorerFragment")
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        activity!!.startActivityForResult(intent, REQUEST_CODE_SD_CARD_URI)
    }

    override fun updateAdapter() {
        Log.d("flow", "updating adapter in ExplorerFragment")
        adapter?.notifyDataSetChanged()
    }

    override fun onPause() {
        Log.d("flow", "pausing ExplorerFragment")
        dialog?.dismiss()
        super.onPause()
    }

    override fun backClicked(): Boolean {
        Log.d("flow", "back clicked in ExplorerFragment")
        return presenter.backClicked()
    }

}
