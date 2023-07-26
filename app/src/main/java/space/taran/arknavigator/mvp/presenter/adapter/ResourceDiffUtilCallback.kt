package dev.arkbuilders.navigator.mvp.presenter.adapter

import androidx.recyclerview.widget.DiffUtil
import space.taran.arklib.ResourceId

class ResourceDiffUtilCallback(
    private val oldItems: List<ResourceId>,
    private val newItems: List<ResourceId>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldItems.size

    override fun getNewListSize(): Int = newItems.size

    override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
        return oldItems[oldPos] == newItems[newPos]
    }

    // due to content-addressing, `id1 = id2` means `content1 = content2`
    override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean =
        areItemsTheSame(oldPos, newPos)
}
