package dev.arkbuilders.navigator.analytics

import android.content.Context
import dagger.Module
import dagger.Provides
import dev.arkbuilders.navigator.analytics.folders.FoldersAnalytics
import dev.arkbuilders.navigator.analytics.folders.FoldersAnalyticsImpl
import dev.arkbuilders.navigator.analytics.gallery.GalleryAnalytics
import dev.arkbuilders.navigator.analytics.gallery.GalleryAnalyticsImpl
import dev.arkbuilders.navigator.analytics.resources.ResourcesAnalytics
import dev.arkbuilders.navigator.analytics.resources.ResourcesAnalyticsImpl
import dev.arkbuilders.navigator.analytics.settings.SettingsAnalytics
import dev.arkbuilders.navigator.analytics.settings.SettingsAnalyticsImpl
import org.matomo.sdk.Tracker
import javax.inject.Singleton

@Module
class AnalyticsModule {

    @Singleton
    @Provides
    fun provideFolderAnalytics(
        matomoTracker: Tracker,
        context: Context
    ): FoldersAnalytics =
        FoldersAnalyticsImpl(matomoTracker = matomoTracker, context = context)

    @Singleton
    @Provides
    fun provideResourcesAnalytics(
        matomoTracker: Tracker
    ): ResourcesAnalytics = ResourcesAnalyticsImpl(matomoTracker)

    @Singleton
    @Provides
    fun provideGalleryAnalytics(
        matomoTracker: Tracker,
    ): GalleryAnalytics = GalleryAnalyticsImpl(matomoTracker)

    @Singleton
    @Provides
    fun provideSettingsAnalytics(
        matomoTracker: Tracker
    ): SettingsAnalytics = SettingsAnalyticsImpl(matomoTracker)
}
