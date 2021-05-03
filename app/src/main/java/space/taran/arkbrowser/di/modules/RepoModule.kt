package space.taran.arkbrowser.di.modules

import android.util.Log
import space.taran.arkbrowser.mvp.model.entity.room.Database
import dagger.Module
import dagger.Provides
import space.taran.arkbrowser.mvp.model.repo.FoldersRepo
import javax.inject.Singleton

@Module
class RepoModule {
    @Singleton
    @Provides
    fun foldersRepo(database: Database): FoldersRepo {
        Log.d("modules", "creating FoldersRepo")
        return FoldersRepo(database.folderDao())
    }

    //todo join with DatabaseModule
}