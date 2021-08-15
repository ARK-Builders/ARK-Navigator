package space.taran.arknavigator.mvp.presenter

import android.util.Log
import com.ekezet.treeview.*
import ru.terrakok.cicerone.Router
import space.taran.arknavigator.utils.FOLDERS_TREE
import space.taran.arknavigator.mvp.model.repo.Folders
import space.taran.arknavigator.mvp.view.item.AddFavoriteHandler
import space.taran.arknavigator.mvp.view.item.FolderViewHolderFactory
import java.lang.IllegalStateException
import java.nio.file.Path

interface FolderTreeNode<N> {
    //it can be relative path or something arbitrary
    val name: String

    val path: Path
    fun children(): List<TreeItem<N>>
}

data class DeviceNode(
    override val name: String,
    override val path: Path,
    val children: List<RootNode>): FolderTreeNode<RootNode> {

    override fun children(): List<TreeItem<RootNode>> {
        return children.map { TreeItem from it }
    }
}

data class RootNode(
    override val name: String,
    override val path: Path,
    val children: List<FavoriteNode>): FolderTreeNode<FavoriteNode> {

    override fun children(): List<TreeItem<FavoriteNode>> {
        return children.map { TreeItem from it }
    }
}

data class FavoriteNode(
    override val name: String,
    override val path: Path,
    val root: Path): FolderTreeNode<Unit> {

    override fun children(): List<TreeItem<Unit>> {
        return emptyList()
    }
}

class FoldersTree(devices: List<Path>,
                  folders: Folders,
                  handler: AddFavoriteHandler,
                  router: Router)
    : TreeViewAdapter(
        FolderViewHolderFactory(handler, router),
        extractFolderDetails(devices, folders)) {

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
                            FavoriteNode(
                                name = it.toString(),
                                root = root,
                                path = root.resolve(it))
                        }

                        Log.d(FOLDERS_TREE, "root $root contains favorites ${favorites.map { it.path }}")
                        RootNode(
                            name = device.relativize(root).toString(),
                            path = root,
                            children = favorites)
                    }

                    Log.d(FOLDERS_TREE, "device $device contains roots ${roots.map { it.path }}")
                    TreeItem from DeviceNode(
                        name = device.getName(1).toString(),
                        path = device,
                        children = roots)
                }
                .toMutableList()
        }
    }
}