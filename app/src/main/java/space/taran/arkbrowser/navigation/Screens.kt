package space.taran.arkbrowser.navigation

import space.taran.arkbrowser.ui.fragments.*
import ru.terrakok.cicerone.android.support.SupportAppScreen
import space.taran.arkbrowser.mvp.model.dao.ResourceId
import space.taran.arkbrowser.mvp.model.repo.ResourcesIndex
import space.taran.arkbrowser.mvp.model.repo.TagsStorage
import space.taran.arkbrowser.mvp.presenter.adapter.ResourcesList
import java.nio.file.Path

class Screens {
    class FoldersScreen: SupportAppScreen() {
        override fun getFragment() = FoldersFragment()
    }

    class ResourcesScreen(val root: Path?, val path: Path?): SupportAppScreen() {
        override fun getFragment() = ResourcesFragment(root, path)
    }

    class GalleryScreen(val index: ResourcesIndex,
                        val storage: TagsStorage,
                        val resources: ResourcesList,
                        val position: Int): SupportAppScreen() {
        override fun getFragment() = GalleryFragment(index, storage, resources, position)
    }
}