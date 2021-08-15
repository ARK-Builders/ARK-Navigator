package space.taran.arknavigator.navigation

import space.taran.arknavigator.ui.fragments.*
import ru.terrakok.cicerone.android.support.SupportAppScreen
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.repo.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.TagsStorage
import space.taran.arknavigator.mvp.presenter.adapter.ResourcesList
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