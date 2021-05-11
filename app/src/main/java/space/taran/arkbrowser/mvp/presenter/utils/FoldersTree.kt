package space.taran.arkbrowser.mvp.presenter.utils

import android.animation.ValueAnimator
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import com.ekezet.treeview.*
import com.ekezet.treeview.TreeViewAdapter.ViewHolder
import com.ekezet.treeview.TreeViewAdapter.TreeItemView
import kotlinx.android.synthetic.main.item_view_folder_tree_root.view.*
import space.taran.arkbrowser.R
import space.taran.arkbrowser.utils.FOLDERS_TREE
import space.taran.arkbrowser.mvp.model.repo.Folders
import java.lang.IllegalStateException
import java.nio.file.Path

//todo simplify
private interface Node<N> {
    val path: Path
    fun children(): List<TreeItem<N>>
}

private data class DeviceNode(
    override val path: Path,
    val children: List<RootNode>): Node<RootNode> {

    override fun children(): List<TreeItem<RootNode>> {
        return children.map { TreeItem from it }
    }
}

private data class RootNode(
    override val path: Path,
    val children: List<FavoriteNode>): Node<FavoriteNode> {

    override fun children(): List<TreeItem<FavoriteNode>> {
        return children.map { TreeItem from it }
    }
}

private data class FavoriteNode(
    override val path: Path): Node<Unit> {
    override fun children(): List<TreeItem<Unit>> {
        return emptyList()
    }
}

class FoldersTree(devices: List<Path>, folders: Folders)
    : TreeViewAdapter(Factory(), extractFolderDetails(devices, folders)) {

    companion object {
        fun extractFolderDetails(_devices: List<Path>, folders: Folders)
        : MutableList<AnyTreeItem> {
            Log.d(FOLDERS_TREE, "preparing FoldersTree to display")
            Log.d(FOLDERS_TREE, "devices = $_devices")
            Log.d(FOLDERS_TREE, "folders = $folders")

            val devices = _devices.toTypedArray()

            return folders.mapKeys { (root, _) ->
                    val idx = devices.indexOfFirst { root.startsWith(it) }
                    if (idx < 0) {
                        throw IllegalStateException("No device contains $root")
                    }

                    idx to root
                }
                .toList()
                .groupBy { (deviceAndRoot, _) ->
                    val (device, _) = deviceAndRoot
                    device
                }
                .map { (idx, folders) ->
                    val device = devices[idx]

                    val roots = folders.map { (deviceAndRoot, _favorites) ->
                        val (_, root) = deviceAndRoot

                        val favorites = _favorites.map {
                            FavoriteNode(path = root.relativize(it))
                        }

                        Log.d(FOLDERS_TREE, "root $root contains favorites ${favorites.map { it.path }}")
                        RootNode(
                            path = device.relativize(root),
                            children = favorites)
                    }

                    Log.d(FOLDERS_TREE, "device $device contains roots ${roots.map { it.path }}")
                    TreeItem from DeviceNode(
                        path = device,
                        children = roots)
                }
                .toMutableList()
        }
    }
}

private class Factory: TreeViewAdapter.ViewHolderFactory {
    override fun createViewHolder(context: Context, viewType: Int): ViewHolder<AnyTreeItemView> {
        Log.d("DEBUG", "device hash: ${DeviceNode::class.hashCode()}")
        Log.d("DEBUG", "root hash: ${RootNode::class.hashCode()}")
        Log.d("DEBUG", "favorite hash: ${FavoriteNode::class.hashCode()}")
        Log.d("DEBUG", "node hash: ${Node::class.hashCode()}")
        val itemView: View = when (viewType) {
            DeviceNode::class.hashCode()   -> DeviceFolderView(context)
            RootNode::class.hashCode()     -> RootFolderView(context)
            FavoriteNode::class.hashCode() -> FavoriteFolderView(context)
            else -> throw IllegalArgumentException("Illegal viewType: $viewType")
        }
        return ViewHolder(itemView)
    }
}

private class DeviceFolderView(context: Context) : FolderView<RootNode, DeviceNode>(context) {
    init {
        inflate(context, R.layout.item_view_folder_tree_device, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    override fun label(): String {
        return "[device]"
    }
}

private class RootFolderView(context: Context) : FolderView<FavoriteNode, RootNode>(context) {
    init {
        inflate(context, R.layout.item_view_folder_tree_root, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    override fun label(): String {
        return "[root]"
    }
}

//todo simplify
private class FavoriteFolderView(context: Context) : FolderView<Unit, FavoriteNode>(context) {
    init {
        inflate(context, R.layout.item_view_folder_tree_favorite, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    override fun label(): String {
        return "[favorite]"
    }
}


//todo smart folding
private abstract class FolderView<C, N: Node<C>>(context: Context)
    : FrameLayout(context), TreeItemView<N> {

    private val animator = ValueAnimator().apply {
        duration = 500L
        addUpdateListener {
            chevronImage.rotation = animatedValue as Float
        }
    }

    abstract fun label(): String

    override fun bind(item: TreeItem<N>, position: Int) {
        //todo: use `position`

        val data = item.data
        nameText.text = data.path.toString()
        typeText.text = label()

        chevronImage.rotation = if (item.isExpanded)
            90F
        else
            0F
    }

    override fun onExpandChildren(
        item: TreeItem<N>,
        position: Int,
        adapter: ITreeViewAdapter) {

        adapter.insertChildren( position, item.data.children())

        animator.setFloatValues(0F, 90F)
        animator.start()
    }

    override fun onCollapseChildren(
        item: TreeItem<N>,
        position: Int,
        adapter: ITreeViewAdapter) {

        super.onCollapseChildren(item, position, adapter)
        animator.setFloatValues(90F, 00F)
        animator.start()
    }
}