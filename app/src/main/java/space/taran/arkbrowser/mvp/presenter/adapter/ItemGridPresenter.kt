package space.taran.arkbrowser.mvp.presenter.adapter

import android.util.Log
import space.taran.arkbrowser.mvp.view.item.FileItemView
import space.taran.arkbrowser.utils.ITEM_GRID

abstract class ItemGridPresenter<Label, Item>(
    private val handler: (Item) -> Unit) {

    abstract fun items(): List<Item>
    abstract fun updateItems(label: Label, items: List<Item>)
    abstract fun bindView(view: FileItemView)

    // returns null if the event wasn't processed
    abstract fun backClicked(): Label?

    fun getCount() = items().size

    fun itemClicked(pos: Int) {
        Log.d(ITEM_GRID, "item $pos clicked")
        handler(items()[pos])
    }
}