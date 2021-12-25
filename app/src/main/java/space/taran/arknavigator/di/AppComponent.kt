package space.taran.arknavigator.di

import dagger.Component
import space.taran.arknavigator.di.modules.AppModule
import space.taran.arknavigator.di.modules.CiceroneModule
import space.taran.arknavigator.di.modules.DatabaseModule
import space.taran.arknavigator.di.modules.RepoModule
import space.taran.arknavigator.mvp.presenter.*
import space.taran.arknavigator.mvp.presenter.adapter.FoldersWalker
import space.taran.arknavigator.mvp.presenter.adapter.ResourcesGridPresenter
import space.taran.arknavigator.mvp.presenter.adapter.folderstree.FoldersTreePresenter
import space.taran.arknavigator.mvp.presenter.dialog.EditTagsDialogPresenter
import space.taran.arknavigator.mvp.view.item.FileItemViewHolder
import space.taran.arknavigator.mvp.view.item.PreviewItemViewHolder
import space.taran.arknavigator.ui.activity.MainActivity
import space.taran.arknavigator.ui.fragments.*
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
    fun inject(foldersTreePresenter: FoldersTreePresenter)
    fun inject(foldersWalker: FoldersWalker)
    fun inject(editTagsDialogPresenter: EditTagsDialogPresenter)
    fun inject(fileItemViewHolder: FileItemViewHolder)
    fun inject(previewItemViewHolder: PreviewItemViewHolder)
}
