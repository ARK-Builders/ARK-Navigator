package space.taran.arkbrowser.mvp.presenter.adapter

import android.util.Log
import kotlinx.collections.immutable.persistentListOf
import space.taran.arkbrowser.utils.ITEM_GRID
import java.lang.AssertionError
import java.lang.IllegalStateException

abstract class ReversibleItemGridPresenter<T>(
    init: List<T>, handler: (T) -> Unit)
        : ItemGridPresenter<T>(handler) {

    private var frames = persistentListOf(init)
    private var depth = 1

    override fun items(): List<T> = frames.last()

    override fun updateItems(items: List<T>) {
        Log.d(ITEM_GRID, "adding a frame of items")
        frames = frames.add(items)
        depth++

        if (depth != frames.size) {
            throw AssertionError()
        }
        Log.d(ITEM_GRID, "new stack depth: $depth")
    }

    override fun backClicked(): Boolean {
        if (depth == 1) {
            return false
        }

        Log.d(ITEM_GRID, "reverting last frame of items")
        frames = frames.removeAt(depth - 1)
        depth--

        if (depth != frames.size) {
            throw AssertionError()
        }
        Log.d(ITEM_GRID, "new stack depth: $depth")
        return true
    }
}