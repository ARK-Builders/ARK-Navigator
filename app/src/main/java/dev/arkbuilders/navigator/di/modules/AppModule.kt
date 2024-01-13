package dev.arkbuilders.navigator.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import dev.arkbuilders.navigator.data.preferences.Preferences
import dev.arkbuilders.navigator.data.preferences.PreferencesImpl
import dev.arkbuilders.navigator.data.utils.DevicePathsExtractor
import dev.arkbuilders.navigator.data.utils.DevicePathsExtractorImpl
import dev.arkbuilders.navigator.presentation.App
import dev.arkbuilders.navigator.presentation.screen.main.MainActivity
import dev.arkbuilders.navigator.presentation.utils.StringProvider
import org.matomo.sdk.Matomo
import org.matomo.sdk.Tracker
import org.matomo.sdk.TrackerBuilder
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

    @Provides
    @Singleton
    fun provideDevicePathsExtractor(application: App): DevicePathsExtractor =
        DevicePathsExtractorImpl(application)

    @Provides
    @Singleton
    fun provideMatomoAnalytics(ctx: Context): Tracker =
        TrackerBuilder.createDefault(
            "https://ark-builders.matomo.cloud/matomo.php",
            1
        )
            .build(Matomo.getInstance(ctx))
}
