package space.taran.arkbrowser.ui

import android.app.Application
import space.taran.arkbrowser.di.AppComponent
import space.taran.arkbrowser.di.DaggerAppComponent
import space.taran.arkbrowser.di.modules.AppModule

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