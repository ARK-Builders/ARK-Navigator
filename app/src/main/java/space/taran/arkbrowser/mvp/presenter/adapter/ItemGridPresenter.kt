package space.taran.arkbrowser.mvp.presenter.adapter

import android.util.Log
import space.taran.arkbrowser.mvp.view.item.FileItemView
import space.taran.arkbrowser.utils.ITEM_GRID

abstract class ItemGridPresenter<T>(
    private val handler: (T) -> Unit) {

    abstract fun items(): List<T>
    abstract fun updateItems(items: List<T>)
    abstract fun bindView(view: FileItemView)

    // returns true if the event was processed
    abstract fun backClicked(): Boolean

    fun getCount() = items().size

    fun itemClicked(pos: Int) {
        Log.d(ITEM_GRID, "item $pos clicked")
        handler(items()[pos])
    }
}