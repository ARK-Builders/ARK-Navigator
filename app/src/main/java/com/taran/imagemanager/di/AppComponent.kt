package com.taran.imagemanager.di

import com.taran.imagemanager.di.modules.AppModule
import com.taran.imagemanager.di.modules.CiceroneModule
import com.taran.imagemanager.di.modules.DatabaseModule
import com.taran.imagemanager.di.modules.RepoModule
import com.taran.imagemanager.mvp.presenter.DetailPresenter
import com.taran.imagemanager.mvp.presenter.ExplorerPresenter
import com.taran.imagemanager.mvp.presenter.HistoryPresenter
import com.taran.imagemanager.ui.MainActivity
import com.taran.imagemanager.ui.fragments.DetailFragment
import com.taran.imagemanager.ui.fragments.ExplorerFragment
import com.taran.imagemanager.ui.fragments.HistoryFragment
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        CiceroneModule::class,
        DatabaseModule::class,
        RepoModule::class
    ]
)

interface AppComponent {
    fun inject(mainActivity: MainActivity)
    fun inject(explorerFragment: ExplorerFragment)
    fun inject(explorerPresenter: ExplorerPresenter)
    fun inject(detailPresenter: DetailPresenter)
    fun inject(detailImageFragment: DetailFragment)
    fun inject(historyPresenter: HistoryPresenter)
    fun inject(historyFragment: HistoryFragment)
}