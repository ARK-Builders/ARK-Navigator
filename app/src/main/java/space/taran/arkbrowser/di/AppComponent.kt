package space.taran.arkbrowser.di

import space.taran.arkbrowser.di.modules.AppModule
import space.taran.arkbrowser.di.modules.CiceroneModule
import space.taran.arkbrowser.di.modules.DatabaseModule
import space.taran.arkbrowser.di.modules.RepoModule
import space.taran.arkbrowser.mvp.presenter.*
import space.taran.arkbrowser.ui.activity.MainActivity
import space.taran.arkbrowser.ui.fragments.*
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
    fun inject(rootFragment: RootFragment)
    fun inject(rootPresenter: RootPresenter)
    fun inject(tagsPresenter: TagsPresenter)
    fun inject(tagsFragment: TagsFragment)
}