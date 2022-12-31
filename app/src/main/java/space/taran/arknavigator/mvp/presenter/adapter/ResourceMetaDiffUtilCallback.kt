package space.taran.arknavigator.mvp.presenter.adapter

import androidx.recyclerview.widget.DiffUtil
import space.taran.arklib.domain.index.ResourceMeta

class ResourceMetaDiffUtilCallback(
    private val oldList: List<ResourceMeta>,
    private val newList: List<ResourceMeta>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int):
        Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int):
        Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
