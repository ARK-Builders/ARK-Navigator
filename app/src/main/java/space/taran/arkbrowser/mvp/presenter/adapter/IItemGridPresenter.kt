package space.taran.arkbrowser.mvp.presenter.adapter

import android.util.Log
import space.taran.arkbrowser.mvp.view.item.FileItemView

abstract class IItemGridPresenter<T>(
    private val handler: (T) -> Unit) {

    abstract fun items(): List<T>
    abstract fun bindView(view: FileItemView)
    abstract fun backClicked()

    fun getCount() = items().size

    fun itemClicked(pos: Int) {
        Log.d("flow", "item $pos clicked in IItemGridPresenter")
        handler(items()[pos])
    }
}