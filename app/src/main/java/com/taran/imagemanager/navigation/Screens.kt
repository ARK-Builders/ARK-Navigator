package com.taran.imagemanager.navigation

import com.taran.imagemanager.mvp.model.entity.Folder
import com.taran.imagemanager.mvp.model.entity.Image
import com.taran.imagemanager.ui.fragments.DetailFragment
import com.taran.imagemanager.ui.fragments.ExplorerFragment
import com.taran.imagemanager.ui.fragments.HistoryFragment
import ru.terrakok.cicerone.android.support.SupportAppScreen

class Screens {
    class ExplorerScreen(val folder: Folder): SupportAppScreen() {
        override fun getFragment() = ExplorerFragment.newInstance(folder)
    }

    class DetailScreen(val images: MutableList<Image>, val pos: Int, val folder: Folder): SupportAppScreen() {
        override fun getFragment() = DetailFragment.newInstance(images, pos, folder)
    }

    class HistoryScreen: SupportAppScreen() {
        override fun getFragment() = HistoryFragment.newInstance()
    }
}