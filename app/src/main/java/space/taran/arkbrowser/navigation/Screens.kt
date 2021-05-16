package space.taran.arkbrowser.navigation

import space.taran.arkbrowser.ui.fragments.*
import ru.terrakok.cicerone.android.support.SupportAppScreen
import space.taran.arkbrowser.mvp.model.entity.room.ResourceId
import java.nio.file.Path

class Screens {
    class DetailScreen(val resources: List<ResourceId>, val pos: Int): SupportAppScreen() {
        override fun getFragment() = DetailFragment.newInstance(resources, pos)
    }

    class FoldersScreen: SupportAppScreen() {
        override fun getFragment() = FoldersFragment()
    }

    class ResourcesScreen(val root: Path?, val path: Path?): SupportAppScreen() {
        override fun getFragment() = ResourcesFragment(root, path)
    }
}