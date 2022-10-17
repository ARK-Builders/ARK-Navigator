package space.taran.arknavigator.di

import dagger.Component
import space.taran.arknavigator.di.modules.AppModule
import space.taran.arknavigator.di.modules.CiceroneModule
import space.taran.arknavigator.di.modules.DatabaseModule
import space.taran.arknavigator.di.modules.RepoModule
import space.taran.arknavigator.mvp.model.backup.StorageBackup
import space.taran.arknavigator.mvp.model.repo.preferences.Preferences
import space.taran.arknavigator.mvp.presenter.FoldersPresenter
import space.taran.arknavigator.mvp.presenter.GalleryPresenter
import space.taran.arknavigator.mvp.presenter.MainPresenter
import space.taran.arknavigator.mvp.presenter.ResourcesPresenter
import space.taran.arknavigator.mvp.presenter.adapter.ResourcesGridPresenter
import space.taran.arknavigator.mvp.presenter.adapter.folderstree.FoldersTreePresenter
import space.taran.arknavigator.mvp.presenter.adapter.tagsselector.TagsSelectorPresenter
import space.taran.arknavigator.mvp.presenter.dialog.EditTagsDialogPresenter
import space.taran.arknavigator.mvp.presenter.dialog.FolderPickerDialogPresenter
import space.taran.arknavigator.mvp.presenter.dialog.SortDialogPresenter
import space.taran.arknavigator.mvp.view.item.FileItemViewHolder
import space.taran.arknavigator.ui.activity.MainActivity
import space.taran.arknavigator.ui.adapter.TagsSelectorAdapter
import space.taran.arknavigator.ui.adapter.previewpager.PreviewItemViewHolder
import space.taran.arknavigator.ui.fragments.FoldersFragment
import space.taran.arknavigator.ui.fragments.GalleryFragment
import space.taran.arknavigator.ui.fragments.ResourcesFragment
import space.taran.arknavigator.ui.fragments.SettingsFragment
import space.taran.arknavigator.ui.fragments.dialog.TagsSortDialogFragment
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
    fun arkBackup(): StorageBackup
    fun preferences(): Preferences

    fun inject(mainActivity: MainActivity)
    fun inject(mainPresenter: MainPresenter)
    fun inject(foldersPresenter: FoldersPresenter)
    fun inject(foldersFragment: FoldersFragment)
    fun inject(resourcesPresenter: ResourcesPresenter)
    fun inject(resourcesFragment: ResourcesFragment)
    fun inject(galleryPresenter: GalleryPresenter)
    fun inject(galleryFragment: GalleryFragment)
    fun inject(settingsFragment: SettingsFragment)
    fun inject(resourcesGridPresenter: ResourcesGridPresenter)
    fun inject(foldersTreePresenter: FoldersTreePresenter)
    fun inject(editTagsDialogPresenter: EditTagsDialogPresenter)
    fun inject(fileItemViewHolder: FileItemViewHolder)
    fun inject(previewItemViewHolder: PreviewItemViewHolder)
    fun inject(folderPickerDialogPresenter: FolderPickerDialogPresenter)
    fun inject(sortDialogPresenter: SortDialogPresenter)
    fun inject(tagsSelectorPresenter: TagsSelectorPresenter)
    fun inject(tagsSelectorAdapter: TagsSelectorAdapter)
    fun inject(tagsSortDialogFragment: TagsSortDialogFragment)
}
