package space.taran.arknavigator.navigation

import ru.terrakok.cicerone.android.support.SupportAppScreen
import space.taran.arknavigator.mvp.model.repo.RootAndFav
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.ui.fragments.FoldersFragment
import space.taran.arknavigator.ui.fragments.GalleryFragment
import space.taran.arknavigator.ui.fragments.ResourcesFragment

class Screens {
    class FoldersScreen : SupportAppScreen() {
        override fun getFragment() = FoldersFragment()
    }

    class ResourcesScreen(val rootAndFav: RootAndFav) : SupportAppScreen() {
        override fun getFragment() = ResourcesFragment.newInstance(rootAndFav)
    }

    class GalleryScreen(
        val rootAndFav: RootAndFav,
        val resources: List<ResourceId>,
        val startAt: Int
    ) : SupportAppScreen() {
        override fun getFragment() = GalleryFragment.newInstance(rootAndFav, resources, startAt)
    }

    class SettingsScreen: SupportAppScreen(){
        override fun getFragment() = SettingsFragment.newInstance()
    }
}
