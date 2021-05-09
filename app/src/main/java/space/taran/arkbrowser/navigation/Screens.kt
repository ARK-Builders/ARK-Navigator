package space.taran.arkbrowser.navigation

import space.taran.arkbrowser.ui.fragments.*
import ru.terrakok.cicerone.android.support.SupportAppScreen
import space.taran.arkbrowser.mvp.model.entity.room.ResourceId
import java.nio.file.Path

class Screens {
    class ExplorerScreen(val folder: Path? = null): SupportAppScreen() {
        override fun getFragment() = ExplorerFragment.newInstance(folder)
    }

    class DetailScreen(val resources: List<ResourceId>, val pos: Int): SupportAppScreen() {
        override fun getFragment() = DetailFragment.newInstance(resources, pos)
    }

    class RootsScreen: SupportAppScreen() {
        override fun getFragment() = RootsFragment()
    }

    class TagsScreen(val resources: Set<ResourceId>): SupportAppScreen() {
        override fun getFragment() = TagsFragment(resources)
    }
}