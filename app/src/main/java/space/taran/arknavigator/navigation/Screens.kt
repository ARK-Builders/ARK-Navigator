package space.taran.arknavigator.navigation

import ru.terrakok.cicerone.android.support.SupportAppScreen
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.preview.PreviewStorage
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorage
import space.taran.arknavigator.ui.fragments.FoldersFragment
import space.taran.arknavigator.ui.fragments.GalleryFragment
import space.taran.arknavigator.ui.fragments.ResourcesFragment
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
                        val previewStorage: PreviewStorage,
                        val selection: List<ResourceMeta>,
                        val position: Int): SupportAppScreen() {
        override fun getFragment() = GalleryFragment(index, storage, previewStorage, selection.toMutableList(), position)
    }
}