package space.taran.arkbrowser.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import space.taran.arkbrowser.R
import space.taran.arkbrowser.mvp.model.entity.File
import space.taran.arkbrowser.mvp.presenter.ExplorerPresenter
import space.taran.arkbrowser.mvp.view.ExplorerView
import space.taran.arkbrowser.ui.App
import space.taran.arkbrowser.ui.MainActivity
import space.taran.arkbrowser.ui.adapter.FileGridRVAdapter
import kotlinx.android.synthetic.main.fragment_explorer.*
import moxy.MvpAppCompatFragment
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter

class ExplorerFragment : MvpAppCompatFragment(), ExplorerView, BackButtonListener {

    companion object {
        const val FOLDER_KEY = "file"

        fun newInstance(folder: File?) = ExplorerFragment().apply {
            arguments = Bundle().apply {
                putParcelable(FOLDER_KEY, folder)
            }
        }
    }

    var dialog: AlertDialog? = null

    @InjectPresenter
    lateinit var presenter: ExplorerPresenter

    @ProvidePresenter
    fun providePresenter() = ExplorerPresenter(arguments!![FOLDER_KEY] as File?).apply {
        App.instance.appComponent.inject(this)
    }

    var adapter: FileGridRVAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_explorer, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        App.instance.appComponent.inject(this)
    }

    override fun onResume() {
        super.onResume()
        presenter.onViewResumed()
    }

    override fun init() {
        rv_files.layoutManager = GridLayoutManager(context, 3)
        adapter = FileGridRVAdapter(presenter.fileGridPresenter)
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
        if (isVisible)
            fab_explorer_fav.visibility = View.VISIBLE
        else
            fab_explorer_fav.visibility = View.GONE
    }

    override fun setTagsFabVisibility(isVisible: Boolean) {
        if (isVisible)
            fab_explorer_tags.visibility = View.VISIBLE
        else
            fab_explorer_tags.visibility = View.GONE
    }

    override fun openFile(uri: String, mimeType: String) {
        val intent = Intent(Intent.ACTION_EDIT)
        val fileUri: Uri = Uri.parse(uri)
        intent.setDataAndType(fileUri, mimeType)
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
        dialog?.dismiss()
    }

    override fun setSelectedTab(pos: Int) {
        (activity as MainActivity).setSelectedTab(pos)
    }

    override fun requestSdCardUri() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        activity!!.startActivityForResult(intent, 2)
    }

    override fun updateAdapter() {
        adapter?.notifyDataSetChanged()
    }

    override fun onPause() {
        dialog?.dismiss()
        super.onPause()
    }

    override fun backClicked() = presenter.backClicked()

}
