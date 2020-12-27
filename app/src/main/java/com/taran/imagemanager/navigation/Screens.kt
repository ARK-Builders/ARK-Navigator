package space.taran.arkbrowser.navigation

import space.taran.arkbrowser.mvp.model.entity.Folder
import space.taran.arkbrowser.mvp.model.entity.Image
import space.taran.arkbrowser.ui.fragments.DetailFragment
import space.taran.arkbrowser.ui.fragments.ExplorerFragment
import space.taran.arkbrowser.ui.fragments.HistoryFragment
import ru.terrakok.cicerone.android.support.SupportAppScreen

class Screens {
    class ExplorerScreen(val folder: Folder): SupportAppScreen() {
        override fun getFragment() = ExplorerFragment.newInstance(folder)
    }

    class DetailScreen(val images: List<Image>, val pos: Int, val folder: Folder): SupportAppScreen() {
        override fun getFragment() = DetailFragment.newInstance(images, pos, folder)
    }

    class HistoryScreen: SupportAppScreen() {
        override fun getFragment() = HistoryFragment.newInstance()
    }
}