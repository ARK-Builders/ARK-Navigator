package com.taran.imagemanager.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.taran.imagemanager.R
import com.taran.imagemanager.mvp.presenter.HistoryPresenter
import com.taran.imagemanager.mvp.view.HistoryView
import com.taran.imagemanager.ui.App
import com.taran.imagemanager.ui.adapter.FileGridRVAdapter
import kotlinx.android.synthetic.main.fragment_history.*
import moxy.MvpAppCompatFragment
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter


class HistoryFragment : MvpAppCompatFragment(), HistoryView {

    companion object {
        fun newInstance() = HistoryFragment()
    }

    @InjectPresenter
    lateinit var presenter: HistoryPresenter

    @ProvidePresenter
    fun providePresenter() =
        HistoryPresenter().apply {
                App.instance.appComponent.inject(this)
            }

    var adapter: FileGridRVAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_history, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        App.instance.appComponent.inject(this)
    }

    override fun init() {
        rv_folders.layoutManager = GridLayoutManager(context, 3)
        adapter = FileGridRVAdapter(presenter.fileGridPresenter)

        rv_folders.adapter = adapter
    }

    override fun updateAdapter() {
        adapter?.notifyDataSetChanged()
    }
}