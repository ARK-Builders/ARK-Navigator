package space.taran.arknavigator.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import space.taran.arknavigator.mvp.model.UserPreferences
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.ui.resource.StringProvider

@Module
class AppModule(val app: App) {

    @Provides
    fun app(): App {
        return app
    }

    @Singleton
    @Provides
    fun stringProvider(app: App): StringProvider {
        return StringProvider(app)
    }

    @Provides
    @Singleton
    fun provideUserPreferences(appContext: Context): UserPreferences =
        UserPreferences(appContext)

    @Singleton
    @Provides
    fun provideContext(): Context {
        return app
    }
}
