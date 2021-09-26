package space.taran.arknavigator.ui.adapter.folderstree

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import space.taran.arknavigator.databinding.ItemViewFolderTreeDeviceBinding
import space.taran.arknavigator.databinding.ItemViewFolderTreeFavoriteBinding
import space.taran.arknavigator.databinding.ItemViewFolderTreeRootBinding
import space.taran.arknavigator.mvp.presenter.adapter.folderstree.DeviceNode
import space.taran.arknavigator.mvp.presenter.adapter.folderstree.FavoriteNode
import space.taran.arknavigator.mvp.presenter.adapter.folderstree.FoldersTreePresenter
import space.taran.arknavigator.mvp.presenter.adapter.folderstree.RootNode

class FoldersTreeAdapter(val presenter: FoldersTreePresenter) :
    RecyclerView.Adapter<FolderNodeView>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderNodeView =
        when (viewType) {
            DeviceNode::class.hashCode() -> DeviceNodeViewHolder(
                ItemViewFolderTreeDeviceBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                ), presenter
            )
            RootNode::class.hashCode() -> RootNodeViewHolder(
                ItemViewFolderTreeRootBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                ), presenter
            )
            FavoriteNode::class.hashCode() -> FavoriteViewHolder(
                ItemViewFolderTreeFavoriteBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                ), presenter
            )
            else -> throw IllegalArgumentException("Illegal viewType: $viewType")
        }


    override fun onBindViewHolder(holder: FolderNodeView, position: Int) {
        presenter.onBind(holder)
    }

    override fun getItemCount() = presenter.getItemCount()

    override fun getItemViewType(position: Int) = presenter.getItemType(position)

    fun dispatchUpdates() {
        presenter.result?.dispatchUpdatesTo(this)
    }
}

