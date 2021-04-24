package space.taran.arkbrowser.navigation

import android.net.Uri
import space.taran.arkbrowser.mvp.model.entity.Resource
import space.taran.arkbrowser.mvp.model.entity.Root
import space.taran.arkbrowser.ui.fragments.*
import ru.terrakok.cicerone.android.support.SupportAppScreen

class Screens {
    class ExplorerScreen(val folder: Uri? = null): SupportAppScreen() {
        override fun getFragment() = ExplorerFragment.newInstance(folder)
    }

    class DetailScreen(val root: Root, val resources: List<Resource>, val pos: Int): SupportAppScreen() {
        override fun getFragment() = DetailFragment.newInstance(root, resources, pos)
    }

    class RootScreen: SupportAppScreen() {
        override fun getFragment() = RootFragment.newInstance()
    }

    class TagsScreen(val rootName: String?, val resources: Set<Resource>): SupportAppScreen() {
        override fun getFragment() = TagsFragment(rootName, resources)
    }
}