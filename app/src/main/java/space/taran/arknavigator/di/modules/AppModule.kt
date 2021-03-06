package space.taran.arknavigator.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import space.taran.arknavigator.mvp.model.repo.preferences.PreferencesImpl
import space.taran.arknavigator.mvp.model.repo.preferences.Preferences
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.ui.resource.StringProvider
import javax.inject.Singleton

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
    fun provideUserPreferences(appContext: Context): Preferences =
        PreferencesImpl(appContext)

    @Singleton
    @Provides
    fun provideContext(): Context {
        return app
    }
}
