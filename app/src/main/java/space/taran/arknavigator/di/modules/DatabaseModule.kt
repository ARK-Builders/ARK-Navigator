package space.taran.arknavigator.di.modules

import android.util.Log
import dagger.Module
import dagger.Provides
import space.taran.arknavigator.mvp.model.dao.Database
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.utils.LogTags.MODULES
import javax.inject.Singleton

@Module
class DatabaseModule {
    @Singleton
    @Provides
    fun database(app: App): Database {
        Log.d(MODULES, "creating Database")
        return Database.build(app)
    }
}
