package com.taran.imagemanager.navigation

import androidx.fragment.app.Fragment
import com.taran.imagemanager.mvp.model.entity.Image
import com.taran.imagemanager.ui.fragments.DetailFragment
import com.taran.imagemanager.ui.fragments.ExplorerFragment
import com.taran.imagemanager.ui.fragments.HistoryFragment
import ru.terrakok.cicerone.android.support.SupportAppScreen

class Screens {
    class ExplorerScreen(val path: String): SupportAppScreen() {
        override fun getFragment() = ExplorerFragment.newInstance(path)
    }

    class DetailScreen(val images: MutableList<Image>, val pos: Int): SupportAppScreen() {
        override fun getFragment() = DetailFragment.newInstance(images, pos)
    }

    class HistoryScreen: SupportAppScreen() {
        override fun getFragment() = HistoryFragment.newInstance()
    }
}