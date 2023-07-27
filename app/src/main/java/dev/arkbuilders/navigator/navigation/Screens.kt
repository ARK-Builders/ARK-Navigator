package dev.arkbuilders.navigator.navigation

import ru.terrakok.cicerone.android.support.SupportAppScreen
import space.taran.arkfilepicker.folders.RootAndFav
import space.taran.arklib.ResourceId
import dev.arkbuilders.navigator.ui.fragments.FoldersFragment
import dev.arkbuilders.navigator.ui.fragments.GalleryFragment
import dev.arkbuilders.navigator.ui.fragments.ResourcesFragment
import dev.arkbuilders.navigator.ui.fragments.SettingsFragment
import dev.arkbuilders.navigator.utils.Tag

class Screens {
    class FoldersScreen : SupportAppScreen() {
        override fun getFragment() = FoldersFragment.newInstance()
    }

    class FoldersScreenRescanRoots : SupportAppScreen() {
        override fun getFragment() = FoldersFragment.newInstance(rescan = true)
    }

    class ResourcesScreen(val rootAndFav: RootAndFav) : SupportAppScreen() {
        override fun getFragment() = ResourcesFragment.newInstance(rootAndFav)
    }

    class ResourcesScreenWithSelectedTag(
        val rootAndFav: RootAndFav,
        val tag: Tag
    ) : SupportAppScreen() {
        override fun getFragment() = ResourcesFragment.newInstanceWithSelectedTag(
            rootAndFav, tag
        )
    }

    class GalleryScreen(
        val rootAndFav: RootAndFav,
        val resources: List<ResourceId>,
        val startAt: Int
    ) : SupportAppScreen() {
        override fun getFragment() =
            GalleryFragment.newInstance(rootAndFav, resources, startAt)
    }

    class GalleryScreenWithSelected(
        val rootAndFav: RootAndFav,
        val resources: List<ResourceId>,
        val startAt: Int,
        val selectedResources: List<ResourceId>
    ) : SupportAppScreen() {
        override fun getFragment() =
            GalleryFragment.newInstance(
                rootAndFav,
                resources,
                startAt,
                true,
                selectedResources
            )
    }

    class SettingsScreen : SupportAppScreen() {
        override fun getFragment() = SettingsFragment.newInstance()
    }
}
