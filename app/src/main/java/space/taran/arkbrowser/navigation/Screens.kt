package space.taran.arkbrowser.navigation

import space.taran.arkbrowser.mvp.model.entity.File
import space.taran.arkbrowser.mvp.model.entity.Root
import space.taran.arkbrowser.mvp.presenter.TagsPresenter
import space.taran.arkbrowser.ui.fragments.*
import ru.terrakok.cicerone.android.support.SupportAppScreen

class Screens {
    class ExplorerScreen(val folder: File? = null): SupportAppScreen() {
        override fun getFragment() = ExplorerFragment.newInstance(folder)
    }

    class DetailScreen(val root: Root, val images: List<File>, val pos: Int): SupportAppScreen() {
        override fun getFragment() = DetailFragment.newInstance(root, images, pos)
    }

    class RootScreen: SupportAppScreen() {
        override fun getFragment() = RootFragment.newInstance()
    }

    class TagsScreen(val root: Root? = null, val files: List<File>? = null, val state: TagsPresenter.State): SupportAppScreen() {
        override fun getFragment() = TagsFragment(root, files, state)
    }
}