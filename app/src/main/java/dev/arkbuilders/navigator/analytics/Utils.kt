package dev.arkbuilders.navigator.analytics

import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper

fun Tracker.trackScreen(build: TrackHelper.() -> TrackHelper.Screen) {
    val matomoTracker = this
    build(TrackHelper.track()).with(matomoTracker)
}

fun Tracker.trackEvent(build: TrackHelper.() -> TrackHelper.EventBuilder) {
    val matomoTracker = this
    build(TrackHelper.track()).with(matomoTracker)
}
