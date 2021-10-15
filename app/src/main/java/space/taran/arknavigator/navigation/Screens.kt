package space.taran.arknavigator.navigation

import space.taran.arknavigator.ui.fragments.*
import ru.terrakok.cicerone.android.support.SupportAppScreen
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.repo.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.TagsStorage
import java.nio.file.Path

class Screens {
    class FoldersScreen: SupportAppScreen() {
        override fun getFragment() = FoldersFragment()
    }

    class ResourcesScreen(val root: Path?, val path: Path?): SupportAppScreen() {
        override fun getFragment() = ResourcesFragment(root, path)
    }

    class GalleryScreen(val resources: List<ResourceId>,
                        val position: Int): SupportAppScreen() {
        override fun getFragment() = GalleryFragment(resources.toMutableList(), position)
    }
}