package com.taran.imagemanager.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import com.taran.imagemanager.R
import com.taran.imagemanager.mvp.presenter.DetailPresenter
import com.taran.imagemanager.mvp.view.DetailView
import com.taran.imagemanager.ui.App
import com.taran.imagemanager.ui.adapter.DetailVPAdapter
import kotlinx.android.synthetic.main.dialog_tags.*
import kotlinx.android.synthetic.main.dialog_tags.view.*
import kotlinx.android.synthetic.main.dialog_tags.view.et_tags
import kotlinx.android.synthetic.main.fragment_detail_view.*
import moxy.MvpAppCompatFragment
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter

class DetailFragment: MvpAppCompatFragment(), DetailView {

    companion object {
        const val PATH_KEY = "path"
        const val POS_KEY = "pos"

        fun newInstance(path: String, pos: Int) = DetailFragment().apply {
            arguments = Bundle().apply {
                putString(PATH_KEY, path)
                putInt(POS_KEY, pos)
            }
        }
    }

    @InjectPresenter
    lateinit var presenter: DetailPresenter

    @ProvidePresenter
    fun providePresenter() = DetailPresenter(
        arguments!!.getString(PATH_KEY, "/"),
        arguments!!.getInt(POS_KEY, 0)
    ).apply {
        App.instance.appComponent.inject(this)
    }

    var adapter: DetailVPAdapter? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_detail_view, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        App.instance.appComponent.inject(this)
    }

    override fun init() {
        adapter = DetailVPAdapter(presenter.detailListPresenter)
        viewPager.adapter = adapter
        fab.setOnClickListener {
            presenter.fabClicked(viewPager.currentItem)
        }
    }

    override fun setCurrentItem(pos: Int) {
        viewPager.setCurrentItem(pos, false)
    }

    override fun updateAdapter() {
        viewPager.adapter?.notifyDataSetChanged()
    }

    override fun showTagsDialog(tags: String) {
        val dialogView = LayoutInflater.from(context!!).inflate(R.layout.dialog_tags, null)
        val alertDialogBuilder = AlertDialog.Builder(context!!).setView(dialogView)
        alertDialogBuilder.show()

        dialogView.et_tags.setText(tags)
        dialogView.et_tags.setOnEditorActionListener { v, actionId, event ->

            if (actionId == EditorInfo.IME_ACTION_DONE) {
                presenter.tagsEdit(viewPager.currentItem, dialogView.et_tags.text.toString())
            }

            false
        }
    }

}