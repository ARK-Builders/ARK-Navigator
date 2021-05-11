package space.taran.arkbrowser.mvp.presenter.utils

import android.animation.ValueAnimator
import android.content.Context
import android.util.Log
import android.widget.FrameLayout
import com.ekezet.treeview.*
import com.ekezet.treeview.TreeViewAdapter.ViewHolder
import com.ekezet.treeview.TreeViewAdapter.TreeItemView
import kotlinx.android.synthetic.main.item_view_folder.view.*
import space.taran.arkbrowser.R
import space.taran.arkbrowser.utils.FOLDERS_TREE
import space.taran.arkbrowser.mvp.model.repo.Folders
import java.lang.IllegalStateException
import java.nio.file.Path

enum class FolderType {
    DEVICE, ROOT, FAVORITE
}

private data class Node(
    val path: Path,
    val type: FolderType,
    val children: List<Node>)

class FoldersTree(devices: List<Path>, folders: Folders):
    TreeViewAdapter(Factory(), extractFolderDetails(devices, folders)) {

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
                            Node(
                                path = root.relativize(it),
                                type = FolderType.FAVORITE,
                                children = listOf())
                        }

                        Log.d(FOLDERS_TREE, "root $root contains favorites ${favorites.map { it.path }}")
                        Node(
                            path = device.relativize(root),
                            type = FolderType.ROOT,
                            children = favorites)
                    }

                    Log.d(FOLDERS_TREE, "device $device contains roots ${roots.map { it.path }}")
                    TreeItem from Node(
                        path = device,
                        type = FolderType.DEVICE,
                        children = roots)
                }
                .toMutableList()
        }
    }
}

private class Factory: TreeViewAdapter.ViewHolderFactory {

    private val typesAmount = FolderType.values().size

    override fun createViewHolder(context: Context, viewType: Int): ViewHolder<AnyTreeItemView> {
        if (viewType < typesAmount) {
            throw IllegalArgumentException("Unknown viewType: $viewType")
        }
        return ViewHolder(FolderView(context))
    }
}

private class FolderView(context: Context)
    : FrameLayout(context), TreeItemView<Node> {

    private val animator = ValueAnimator().apply {
        duration = 500L
        addUpdateListener {
            chevronImage.rotation = animatedValue as Float
        }
    }

    init {
        inflate(context, R.layout.item_view_folder, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    override fun bind(item: TreeItem<Node>, position: Int) {
        //todo: use `position`

        val data = item.data
        nameText.text = data.path.toString()
        typeText.text = when (data.type) {
            FolderType.DEVICE   -> "[device]"
            FolderType.ROOT     -> "[root]"
            FolderType.FAVORITE -> "[favorite]"
        }

        chevronImage.rotation = if (item.isExpanded)
            90F
        else
            0F
    }

    override fun onExpandChildren(
        item: TreeItem<Node>,
        position: Int,
        adapter: ITreeViewAdapter) {

        //todo: adapter.insertChildren(position, Repo.getAlbums(item.data.id).map { TreeItem from it })
        animator.setFloatValues(0F, 90F)
        animator.start()
    }

    override fun onCollapseChildren(
        item: TreeItem<Node>,
        position: Int,
        adapter: ITreeViewAdapter) {

        super.onCollapseChildren(item, position, adapter)
        animator.setFloatValues(90F, 00F)
        animator.start()
    }
}