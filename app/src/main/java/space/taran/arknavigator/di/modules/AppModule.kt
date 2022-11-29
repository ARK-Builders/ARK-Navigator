package space.taran.arknavigator.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import space.taran.arknavigator.mvp.model.repo.preferences.Preferences
import space.taran.arknavigator.mvp.model.repo.preferences.PreferencesImpl
import space.taran.arknavigator.ui.resource.StringProvider
import javax.inject.Singleton

@Module
class AppModule {

    @Singleton
    @Provides
    fun stringProvider(ctx: Context): StringProvider {
        return StringProvider(ctx)
    }

    @Provides
    @Singleton
    fun provideUserPreferences(ctx: Context): Preferences =
        PreferencesImpl(ctx)
}
