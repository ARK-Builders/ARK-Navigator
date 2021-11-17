package space.taran.arknavigator.navigation

import ru.terrakok.cicerone.android.support.SupportAppScreen
import space.taran.arknavigator.mvp.model.repo.RootAndFav
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorage
import space.taran.arknavigator.ui.fragments.FoldersFragment
import space.taran.arknavigator.ui.fragments.GalleryFragment
import space.taran.arknavigator.ui.fragments.ResourcesFragment

class Screens {
    class FoldersScreen: SupportAppScreen() {
        override fun getFragment() = FoldersFragment()
    }

    class ResourcesScreen(val rootAndFav: RootAndFav): SupportAppScreen() {
        override fun getFragment() = ResourcesFragment.newInstance(rootAndFav)
    }

    class GalleryScreen(val index: ResourcesIndex,
                        val storage: TagsStorage,
                        val selection: List<ResourceMeta>,
                        val position: Int): SupportAppScreen() {
        override fun getFragment() = GalleryFragment(index, storage, selection.toMutableList(), position)
    }
}