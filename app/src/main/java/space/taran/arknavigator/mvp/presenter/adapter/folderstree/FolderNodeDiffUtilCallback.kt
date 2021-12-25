package space.taran.arknavigator.mvp.presenter.adapter.folderstree

import androidx.recyclerview.widget.DiffUtil

class FolderNodeDiffUtilCallback(
    private val oldList: List<FolderNode>,
    private val newList: List<FolderNode>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].path == newList[newItemPosition].path
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].path == newList[newItemPosition].path
    }
}
