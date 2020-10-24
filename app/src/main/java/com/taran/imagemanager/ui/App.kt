package com.taran.imagemanager.ui

import android.app.Application
import com.taran.imagemanager.di.AppComponent
import com.taran.imagemanager.di.DaggerAppComponent
import com.taran.imagemanager.di.modules.AppModule

class App: Application() {

    companion object {
        lateinit var instance: App
    }

    lateinit var appComponent: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()

        instance = this

        appComponent = DaggerAppComponent.builder()
            .appModule(AppModule(this))
            .build()
    }
}