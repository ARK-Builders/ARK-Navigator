package space.taran.arknavigator.mvp.presenter.adapter.folderstree

import android.util.Log
import androidx.recyclerview.widget.DiffUtil
import java.nio.file.Path
import javax.inject.Inject
import space.taran.arknavigator.mvp.model.repo.Folders
import space.taran.arknavigator.mvp.model.repo.RootAndFav
import space.taran.arknavigator.mvp.view.FoldersView
import space.taran.arknavigator.navigation.AppRouter
import space.taran.arknavigator.navigation.Screens
import space.taran.arknavigator.ui.adapter.folderstree.FolderNodeView
import space.taran.arknavigator.utils.LogTags.FOLDERS_TREE

class FoldersTreePresenter(
    val viewState: FoldersView,
    val onAddFolderListener: (path: Path) -> Unit
) {
    @Inject
    lateinit var router: AppRouter

    private var nodes = mutableListOf<FolderNode>()
    private var newNodes = mutableListOf<FolderNode>()
    var result: DiffUtil.DiffResult? = null
        private set

    fun onBind(view: FolderNodeView) {
        val node = nodes[view.position()]
        view.setName(node.name)
        view.setExpanded(node.isExpanded)
    }

    fun onItemClick(view: FolderNodeView) {
        val node = nodes[view.position()]
        if (node.isExpanded)
            removeChildrenCascade(node)
        else
            insertChildren(node, view.position())

        view.setExpanded(node.isExpanded)
        calculateDiffAndNotifyAdapter()
    }

    fun onNavigateBtnClick(view: FolderNodeView) {
        when (val node = nodes[view.position()]) {
            is DeviceNode -> { }
            is RootNode ->
                router
                    .navigateTo(
                        Screens.ResourcesScreen(
                            RootAndFav(node.path.toString(), null)
                        )
                    )
            is FavoriteNode ->
                router
                    .navigateTo(
                        Screens.ResourcesScreen(
                            RootAndFav(node.root.toString(), node.path.toString())
                        )
                    )
        }
    }

    fun onAddFolderBtnClick(pos: Int) {
        onAddFolderListener(nodes[pos].path)
    }

    fun getItemCount() = nodes.size

    fun getItemType(pos: Int) = nodes[pos].type

    fun updateNodes(devices: List<Path>, folders: Folders) {
        val _newNodes = buildNodes(devices, folders)
        // _newNodes is a list with device nodes only, in "collapsed state"
        restoreExpandedState(_newNodes)
        calculateDiffAndNotifyAdapter()
    }

    private fun insertChildren(parent: FolderNode, parentPos: Int) {
        parent.isExpanded = true
        newNodes.addAll(parentPos + 1, parent.children)
    }

    private fun removeChildrenCascade(parent: FolderNode) {
        if (!parent.isExpanded)
            return

        parent.isExpanded = false
        parent.children.forEach {
            removeChildrenCascade(it)
            newNodes.remove(it)
        }
    }

    private fun restoreExpandedState(_newNodes: MutableList<FolderNode>) {
        val tmpNodes = mutableListOf<FolderNode>()
        _newNodes.forEach { newNode ->
            tmpNodes.add(newNode)
            restoreNode(newNode, tmpNodes)
        }
        newNodes = tmpNodes
    }

    private fun restoreNode(node: FolderNode, tmpNodes: MutableList<FolderNode>) {
        val oldNode = nodes.find { it.path == node.path }
        val pos = tmpNodes.indexOf(node)
        oldNode?.let {
            if (it.isExpanded) {
                node.isExpanded = true
                tmpNodes.addAll(pos + 1, node.children)
            }
        }
        node.children.forEach { children ->
            restoreNode(children, tmpNodes)
        }
    }

    private fun calculateDiffAndNotifyAdapter() {
        result = DiffUtil.calculateDiff(
            FolderNodeDiffUtilCallback(nodes, newNodes)
        )
        nodes = newNodes.toMutableList()
        viewState.updateFoldersTree()
    }

    private fun buildNodes(devices: List<Path>, folders: Folders):
        MutableList<FolderNode> {
        Log.d(FOLDERS_TREE, "preparing FoldersTree to display")
        Log.d(FOLDERS_TREE, "devices = $devices")
        Log.d(FOLDERS_TREE, "folders = $folders")

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
            }.map { (idx, folders) ->
                val device = devices[idx]

                val roots = folders.map { (deviceAndRoot, _favorites) ->
                    val (_, root) = deviceAndRoot

                    val favorites = _favorites.map {
                        FavoriteNode(
                            it.toString(),
                            root.resolve(it),
                            root
                        )
                    }

                    Log.d(
                        FOLDERS_TREE,
                        "root $root contains favorites ${
                        favorites.map { it.path }}"
                    )
                    RootNode(
                        device.relativize(root).toString(),
                        root,
                        favorites
                    )
                }

                Log.d(
                    FOLDERS_TREE,
                    "device $device contains roots ${
                    roots.map { it.path }}"
                )
                DeviceNode(
                    device.getName(1).toString(),
                    device,
                    roots
                )
            }
            .toMutableList()
    }
}
