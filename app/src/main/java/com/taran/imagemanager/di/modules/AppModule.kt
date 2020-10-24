package com.taran.imagemanager.di.modules

import com.taran.imagemanager.ui.App
import dagger.Module
import dagger.Provides

@Module
class AppModule(val app: App) {

    @Provides
    fun app(): App {
        return app
    }

}