package dev.arkbuilders.navigator.di

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import space.taran.arkfilepicker.folders.FoldersRepo
import dev.arkbuilders.navigator.di.modules.AppModule
import dev.arkbuilders.navigator.di.modules.CiceroneModule
import dev.arkbuilders.navigator.di.modules.RepoModule
import dev.arkbuilders.navigator.mvp.model.backup.StorageBackup
import dev.arkbuilders.navigator.mvp.model.repo.preferences.Preferences
import dev.arkbuilders.navigator.mvp.presenter.FoldersPresenter
import dev.arkbuilders.navigator.mvp.presenter.GalleryPresenter
import dev.arkbuilders.navigator.mvp.presenter.MainPresenter
import dev.arkbuilders.navigator.mvp.presenter.ResourcesPresenter
import dev.arkbuilders.navigator.mvp.presenter.adapter.ResourcesGridPresenter
import dev.arkbuilders.navigator.mvp.presenter.adapter.tagsselector.TagsSelectorPresenter
import dev.arkbuilders.navigator.mvp.presenter.dialog.EditTagsDialogPresenter
import dev.arkbuilders.navigator.mvp.presenter.dialog.SortDialogPresenter
import dev.arkbuilders.navigator.mvp.view.item.FileItemViewHolder
import dev.arkbuilders.navigator.ui.App
import dev.arkbuilders.navigator.ui.activity.MainActivity
import dev.arkbuilders.navigator.ui.adapter.TagsSelectorAdapter
import dev.arkbuilders.navigator.ui.adapter.previewpager.PreviewImageViewHolder
import dev.arkbuilders.navigator.ui.fragments.FoldersFragment
import dev.arkbuilders.navigator.ui.fragments.GalleryFragment
import dev.arkbuilders.navigator.ui.fragments.ResourcesFragment
import dev.arkbuilders.navigator.ui.fragments.SettingsFragment
import dev.arkbuilders.navigator.ui.fragments.dialog.RootPickerDialogFragment
import dev.arkbuilders.navigator.ui.fragments.dialog.TagsSortDialogFragment
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        CiceroneModule::class,
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
    fun inject(editTagsDialogPresenter: EditTagsDialogPresenter)
    fun inject(fileItemViewHolder: FileItemViewHolder)
    fun inject(previewImageViewHolder: PreviewImageViewHolder)
    fun inject(sortDialogPresenter: SortDialogPresenter)
    fun inject(tagsSelectorPresenter: TagsSelectorPresenter)
    fun inject(tagsSelectorAdapter: TagsSelectorAdapter)
    fun inject(tagsSortDialogFragment: TagsSortDialogFragment)
    fun inject(rootPickerDialogFragment: RootPickerDialogFragment)

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance app: App,
            @BindsInstance context: Context,
            @BindsInstance foldersRepo: FoldersRepo
        ): AppComponent
    }
}
