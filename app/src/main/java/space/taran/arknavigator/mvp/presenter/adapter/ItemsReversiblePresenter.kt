package space.taran.arknavigator.mvp.presenter.adapter

import android.util.Log
import kotlinx.collections.immutable.persistentListOf
import space.taran.arknavigator.utils.ITEMS_CONTAINER
import java.lang.AssertionError

abstract class ItemsReversiblePresenter<Label,Item,View>(
    label: Label, items: List<Item>, handler: ItemClickHandler<Item>)
        : ItemsClickablePresenter<Item,View>(handler) {

    private inner class Frame(val label: Label, val items: List<Item>)

    private var frames = persistentListOf(Frame(label, items))
    private var depth = 1

    fun label(): Label = frames.last().label

    override fun items(): List<Item> = frames.last().items

    override fun updateItems(items: List<Item>) {
        throw UnsupportedOperationException("Use `updateItems` with `label`")
    }

    fun updateItems(label: Label, items: List<Item>) {
        Log.d(ITEMS_CONTAINER, "adding a frame of items")
        frames = frames.add(Frame(label, items))
        depth++

        if (depth != frames.size) {
            throw AssertionError()
        }
        Log.d(ITEMS_CONTAINER, "new stack depth: $depth")
    }

    fun backClicked(): Label? {
        if (depth == 1) {
            return null
        }

        Log.d(ITEMS_CONTAINER, "reverting last frame of items")
        frames = frames.removeAt(depth - 1)
        depth--

        if (depth != frames.size) {
            throw AssertionError()
        }
        Log.d(ITEMS_CONTAINER, "new stack depth: $depth")
        return frames.last().label
    }
}