package space.taran.arkbrowser.mvp.view.item

import android.animation.ValueAnimator
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import com.ekezet.treeview.AnyTreeItemView
import com.ekezet.treeview.ITreeViewAdapter
import com.ekezet.treeview.TreeItem
import com.ekezet.treeview.TreeViewAdapter
import kotlinx.android.synthetic.main.item_view_folder_tree_root.view.*
import ru.terrakok.cicerone.Router
import space.taran.arkbrowser.R
import space.taran.arkbrowser.mvp.presenter.DeviceNode
import space.taran.arkbrowser.mvp.presenter.FavoriteNode
import space.taran.arkbrowser.mvp.presenter.FolderTreeNode
import space.taran.arkbrowser.mvp.presenter.RootNode
import space.taran.arkbrowser.navigation.Screens
import space.taran.arkbrowser.utils.FOLDERS_TREE
import java.nio.file.Path

typealias AddFavoriteHandler = (Path) -> Unit

class FolderViewHolderFactory(
    private val handler: AddFavoriteHandler,
    private val router: Router)
    : TreeViewAdapter.ViewHolderFactory {

    override fun createViewHolder(context: Context, viewType: Int): TreeViewAdapter.ViewHolder<AnyTreeItemView> {
        val itemView: View = when (viewType) {
            DeviceNode::class.hashCode()   -> DeviceFolderView(context)
            RootNode::class.hashCode()     -> RootFolderView(context, handler, router)
            FavoriteNode::class.hashCode() -> FavoriteFolderView(context, router)
            else -> throw IllegalArgumentException("Illegal viewType: $viewType")
        }
        return TreeViewAdapter.ViewHolder(itemView)
    }
}

//todo: *EITHER* make sure that single-argument constructor is never called
// (by handling all such situations and re-creating views from the top)
// *OR* get rid of `router` argument somehow

private class FavoriteFolderView(context: Context,
                                 private val router: Router)
    : FrameLayout(context), TreeViewAdapter.TreeItemView<FavoriteNode> {

    init {
        inflate(context, R.layout.item_view_folder_tree_favorite, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    override fun bind(item: TreeItem<FavoriteNode>, position: Int) {
        Log.d(FOLDERS_TREE, "binding ${item.data}")

        name_txt.text = item.data.name
        type_txt.text = FAVORITE_LABEL

        this.navigate_btn.setOnClickListener {
            Log.d(FOLDERS_TREE, "navigating to path ${item.data.path} under root ${item.data.root}")
            router.navigateTo(Screens.ResourcesScreen(item.data.root, item.data.path))
        }
    }

    override fun onExpandChildren(item: TreeItem<FavoriteNode>, position: Int,
                                  adapter: ITreeViewAdapter) {
        //non-expandable
    }
}

private class RootFolderView(context: Context,
                             private val picker: AddFavoriteHandler,
                             private val router: Router)
    : ExpandableFolderView<FavoriteNode, RootNode>(context) {

    init {
        inflate(context, R.layout.item_view_folder_tree_root, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    override fun bind(item: TreeItem<RootNode>, position: Int) {
        super.bind(item, position)

        this.add_btn.setOnClickListener {
            Log.d(FOLDERS_TREE, "choosing a favorite under ${item.data.path}")
            picker(item.data.path)
        }

        this.navigate_btn.setOnClickListener {
            Log.d(FOLDERS_TREE, "navigating to root ${item.data.path}")
            router.navigateTo(Screens.ResourcesScreen(item.data.path, null))
        }
    }

    override fun label(): String {
        return ROOT_LABEL
    }
}

private class DeviceFolderView(context: Context)
    : ExpandableFolderView<RootNode, DeviceNode>(context) {

    init {
        inflate(context, R.layout.item_view_folder_tree_device, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    override fun label(): String {
        return DEVICE_LABEL
    }
}

//todo smart unfolding
private abstract class ExpandableFolderView<C, N: FolderTreeNode<C>>(context: Context)
    : FrameLayout(context), TreeViewAdapter.TreeItemView<N> {

    private val animator = ValueAnimator().apply {
        duration = 500L
        addUpdateListener {
            chevron.rotation = animatedValue as Float
        }
    }

    abstract fun label(): String

    override fun bind(item: TreeItem<N>, position: Int) {
        Log.d(FOLDERS_TREE, "binding ${item.data}")

        name_txt.text = item.data.name
        type_txt.text = label()

        chevron.rotation = if (item.isExpanded)
            90F
        else
            0F
    }

    override fun onExpandChildren(item: TreeItem<N>,
                                  position: Int, adapter: ITreeViewAdapter) {

        Log.d(FOLDERS_TREE, "expanding ${item.data}")
        Log.d(FOLDERS_TREE, "children are ${item.data.children()}")

        adapter.insertChildren( position, item.data.children())

        animator.setFloatValues(0F, 90F)
        animator.start()
    }

    override fun onCollapseChildren(item: TreeItem<N>,
                                    position: Int, adapter: ITreeViewAdapter) {

        Log.d(FOLDERS_TREE, "collapsing ${item.data}")

        super.onCollapseChildren(item, position, adapter)
        animator.setFloatValues(90F, 00F)
        animator.start()
    }
}

private const val DEVICE_LABEL = "[device]"
private const val ROOT_LABEL = "[root]"
private const val FAVORITE_LABEL = "[favorite]"