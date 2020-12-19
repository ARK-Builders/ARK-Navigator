package com.taran.imagemanager.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.taran.imagemanager.R
import com.taran.imagemanager.mvp.model.entity.Folder
import com.taran.imagemanager.mvp.presenter.ExplorerPresenter
import com.taran.imagemanager.mvp.view.ExplorerView
import com.taran.imagemanager.ui.App
import com.taran.imagemanager.ui.adapter.FileGridRVAdapter
import kotlinx.android.synthetic.main.fragment_explorer.*
import moxy.MvpAppCompatFragment
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter

class ExplorerFragment: MvpAppCompatFragment(), ExplorerView {

    companion object {
        const val FOLDER_KEY = "folder"

        fun newInstance(folder: Folder) = ExplorerFragment().apply {
            arguments = Bundle().apply {
                putParcelable(FOLDER_KEY, folder)
            }
        }
    }

    var dialog: AlertDialog? = null

    @InjectPresenter
    lateinit var presenter: ExplorerPresenter

    @ProvidePresenter
    fun providePresenter() = ExplorerPresenter(arguments!![FOLDER_KEY] as Folder).apply {
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

    override fun init() {
        rv_files.layoutManager = GridLayoutManager(context, 3)
        adapter = FileGridRVAdapter(presenter.fileGridPresenter)

        fab.setOnClickListener {
            presenter.fabClicked()
        }
        rv_files.adapter = adapter
    }

    override fun setFabVisibility(isVisible: Boolean) {
        if (isVisible)
            fab.visibility = View.VISIBLE
        else
            fab.visibility = View.GONE
    }

    override fun showDialog() {
        dialog = MaterialAlertDialogBuilder(context)
            .setTitle("Do you want to add a folder to the home screen?")
            .setPositiveButton("OK") { dialog, which ->
                presenter.favoriteChanged()
            }
            .setNegativeButton("Cancel") { dialog, which -> }
            .show()

        dialog!!.setOnCancelListener {
            presenter.dismissDialog()
        }
    }

    override fun closeDialog() {
        dialog?.dismiss()
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

}