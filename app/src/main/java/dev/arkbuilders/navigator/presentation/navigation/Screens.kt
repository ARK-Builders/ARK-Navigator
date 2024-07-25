package dev.arkbuilders.navigator.presentation.navigation

import ru.terrakok.cicerone.android.support.SupportAppScreen
import dev.arkbuilders.arklib.data.folders.RootAndFav
import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.navigator.presentation.screen.folders.FoldersFragment
import dev.arkbuilders.navigator.presentation.screen.gallery.GalleryFragment
import dev.arkbuilders.navigator.presentation.screen.resources.ResourcesFragment
import dev.arkbuilders.navigator.presentation.screen.settings.SettingsFragment
import dev.arkbuilders.arklib.user.tags.Tag

class Screens {
    class FoldersScreen : SupportAppScreen() {
        override fun getFragment() = FoldersFragment.newInstance()
    }

    class FoldersScreenRescanRoots : SupportAppScreen() {
        override fun getFragment() = FoldersFragment.newInstance(rescan = true)
    }

    class ResourcesScreen(private val rootAndFav: RootAndFav) : SupportAppScreen() {
        override fun getFragment() = ResourcesFragment.newInstance(rootAndFav)
    }

    class ResourcesScreenWithSelectedTag(
        private val rootAndFav: RootAndFav,
        val tag: Tag
    ) : SupportAppScreen() {
        override fun getFragment() = ResourcesFragment.newInstanceWithSelectedTag(
            rootAndFav,
            tag
        )
    }

    class GalleryScreen(
        private val rootAndFav: RootAndFav,
        val resources: List<ResourceId>,
        private val startAt: Int
    ) : SupportAppScreen() {
        override fun getFragment() =
            GalleryFragment.newInstance(rootAndFav, resources, startAt)
    }

    class GalleryScreenWithSelected(
        private val rootAndFav: RootAndFav,
        val resources: List<ResourceId>,
        private val startAt: Int,
        private val selectedResources: List<ResourceId>
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
