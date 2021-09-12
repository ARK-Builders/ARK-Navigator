package space.taran.arknavigator.di

import space.taran.arknavigator.di.modules.AppModule
import space.taran.arknavigator.di.modules.CiceroneModule
import space.taran.arknavigator.di.modules.DatabaseModule
import space.taran.arknavigator.di.modules.RepoModule
import space.taran.arknavigator.mvp.presenter.*
import space.taran.arknavigator.ui.activity.MainActivity
import space.taran.arknavigator.ui.fragments.*
import dagger.Component
import space.taran.arknavigator.mvp.presenter.adapter.ResourcesGridPresenter
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
    fun inject(foldersPresenter: FoldersPresenter)
    fun inject(foldersFragment: FoldersFragment)
    fun inject(resourcesPresenter: ResourcesPresenter)
    fun inject(resourcesFragment: ResourcesFragment)
    fun inject(galleryPresenter: GalleryPresenter)
    fun inject(galleryFragment: GalleryFragment)
    fun inject(resourcesGridPresenter: ResourcesGridPresenter)
}