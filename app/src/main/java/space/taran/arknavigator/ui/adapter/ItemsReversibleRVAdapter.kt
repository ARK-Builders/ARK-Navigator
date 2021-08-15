package space.taran.arknavigator.ui.adapter

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import space.taran.arknavigator.mvp.presenter.adapter.ItemsReversiblePresenter
import space.taran.arknavigator.utils.ITEMS_CONTAINER

//todo: I guess, tags cloud will be reversible too

abstract class ItemsReversibleRVAdapter<Label,Item,View, Holder: RecyclerView.ViewHolder>(
    private val presenter: ItemsReversiblePresenter<Label,Item,View>)
    : RecyclerView.Adapter<Holder>() {

    override fun getItemCount() = presenter.getCount()

    //todo: isn't it violation of adapter contract?
    fun updateItems(label: Label, items: List<Item>) {
        Log.d(ITEMS_CONTAINER, "update requested")
        presenter.updateItems(label, items)
        this.notifyDataSetChanged()
    }

    open fun backClicked(): Label? {
        Log.d(ITEMS_CONTAINER, "[back] clicked")
        val label = presenter.backClicked()
        if (label != null) {
            this.notifyDataSetChanged()
        }
        return label
    }

    fun getLabel() = presenter.label()
}
