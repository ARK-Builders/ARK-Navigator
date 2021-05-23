package space.taran.arkbrowser.mvp.presenter.adapter

import android.util.Log
import space.taran.arkbrowser.utils.ITEMS_CONTAINER

typealias ItemClickHandler<Item> = (Int, Item) -> Unit

abstract class ItemsClickablePresenter<Item,View>(
    private val handler: ItemClickHandler<Item>)
    : ItemsPresenter<Item,View>() {

    fun itemClicked(pos: Int) {
        Log.d(ITEMS_CONTAINER, "item $pos clicked")
        handler(pos, items()[pos])
    }
}