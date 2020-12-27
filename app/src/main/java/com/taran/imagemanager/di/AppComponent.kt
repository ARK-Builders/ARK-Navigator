package space.taran.arkbrowser.di

import space.taran.arkbrowser.di.modules.AppModule
import space.taran.arkbrowser.di.modules.CiceroneModule
import space.taran.arkbrowser.di.modules.DatabaseModule
import space.taran.arkbrowser.di.modules.RepoModule
import space.taran.arkbrowser.mvp.presenter.DetailPresenter
import space.taran.arkbrowser.mvp.presenter.ExplorerPresenter
import space.taran.arkbrowser.mvp.presenter.HistoryPresenter
import space.taran.arkbrowser.mvp.presenter.MainPresenter
import space.taran.arkbrowser.ui.MainActivity
import space.taran.arkbrowser.ui.fragments.DetailFragment
import space.taran.arkbrowser.ui.fragments.ExplorerFragment
import space.taran.arkbrowser.ui.fragments.HistoryFragment
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
    fun inject(mainPresenter: MainPresenter)
    fun inject(explorerFragment: ExplorerFragment)
    fun inject(explorerPresenter: ExplorerPresenter)
    fun inject(detailPresenter: DetailPresenter)
    fun inject(detailImageFragment: DetailFragment)
    fun inject(historyPresenter: HistoryPresenter)
    fun inject(historyFragment: HistoryFragment)
}