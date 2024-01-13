package dev.arkbuilders.navigator.analytics

import android.content.Context
import dagger.Module
import dagger.Provides
import dev.arkbuilders.navigator.analytics.folders.FoldersAnalytics
import dev.arkbuilders.navigator.analytics.folders.impl.FoldersAnalyticsImpl
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
}
