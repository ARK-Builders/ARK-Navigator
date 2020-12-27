package space.taran.arkbrowser.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import space.taran.arkbrowser.R
import space.taran.arkbrowser.mvp.presenter.HistoryPresenter
import space.taran.arkbrowser.mvp.view.HistoryView
import space.taran.arkbrowser.ui.App
import space.taran.arkbrowser.ui.MainActivity
import space.taran.arkbrowser.ui.adapter.FileGridRVAdapter
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
        (activity as MainActivity).setTitle("Home", false)
        rv_folders.adapter = adapter
    }

    override fun updateAdapter() {
        adapter?.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        presenter.onViewResumed()
    }
}