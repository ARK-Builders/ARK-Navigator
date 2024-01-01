package dev.arkbuilders.navigator.analytics

import dagger.Module
import dagger.Provides
import dev.arkbuilders.navigator.analytics.folders.FoldersAnalytics
import dev.arkbuilders.navigator.analytics.folders.FoldersAnalyticsImpl
import org.matomo.sdk.Tracker
import javax.inject.Singleton

@Module
class AnalyticsModule {

    @Singleton
    @Provides
    fun provideFolderAnalytics(matomoTracker: Tracker): FoldersAnalytics =
        FoldersAnalyticsImpl(matomoTracker)
}
