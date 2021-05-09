package space.taran.arkbrowser.mvp.presenter.adapter

import android.util.Log
import kotlinx.collections.immutable.persistentListOf
import space.taran.arkbrowser.utils.ITEM_GRID
import java.lang.AssertionError

abstract class ReversibleItemGridPresenter<Label,Item>(
    label: Label, items: List<Item>, handler: (Item) -> Unit)
        : ItemGridPresenter<Label, Item>(handler) {

    private inner class Frame(val label: Label, val items: List<Item>)

    private var frames = persistentListOf(Frame(label, items))
    private var depth = 1

    override fun label(): Label = frames.last().label

    override fun items(): List<Item> = frames.last().items

    override fun updateItems(label: Label, items: List<Item>) {
        Log.d(ITEM_GRID, "adding a frame of items")
        frames = frames.add(Frame(label, items))
        depth++

        if (depth != frames.size) {
            throw AssertionError()
        }
        Log.d(ITEM_GRID, "new stack depth: $depth")
    }

    override fun backClicked(): Label? {
        if (depth == 1) {
            return null
        }

        Log.d(ITEM_GRID, "reverting last frame of items")
        frames = frames.removeAt(depth - 1)
        depth--

        if (depth != frames.size) {
            throw AssertionError()
        }
        Log.d(ITEM_GRID, "new stack depth: $depth")
        return frames.last().label
    }
}