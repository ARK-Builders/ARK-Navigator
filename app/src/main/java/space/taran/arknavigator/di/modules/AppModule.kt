package space.taran.arknavigator.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.utils.DataStoreManager
import javax.inject.Singleton


@Module
class AppModule(val app: App, val context: Context) {

    @Provides
    fun app(): App {
        return app
    }

    @Provides
    @Singleton
    fun provideDataStoreManager(appContext: Context): DataStoreManager
            = DataStoreManager(appContext)

    @Singleton
    @Provides
    fun provideContext(): Context {
        return context
    }

}