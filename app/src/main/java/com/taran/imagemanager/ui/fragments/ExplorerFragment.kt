package com.taran.imagemanager.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.taran.imagemanager.R
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
        const val PATH_KEY = "path"

        fun newInstance(path: String) = ExplorerFragment().apply {
            arguments = Bundle().apply {
                putString(PATH_KEY, path)
            }
        }
    }

    @InjectPresenter
    lateinit var presenter: ExplorerPresenter

    @ProvidePresenter
    fun providePresenter() = ExplorerPresenter(arguments!!.getString(PATH_KEY, "/")).apply {
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

        rv_files.adapter = adapter
    }

    override fun updateAdapter() {
        adapter?.notifyDataSetChanged()
    }

}