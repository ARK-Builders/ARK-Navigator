package space.taran.arknavigator.ui

import android.app.Application
import space.taran.arknavigator.di.AppComponent
import space.taran.arknavigator.di.DaggerAppComponent
import space.taran.arknavigator.di.modules.AppModule
import timber.log.Timber

class App: Application() {

    companion object {
        lateinit var instance: App
    }

    lateinit var appComponent: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()

        instance = this

        Timber.plant(Timber.DebugTree())

        appComponent = DaggerAppComponent.builder()
            .appModule(AppModule(this))
            .build()
    }
}