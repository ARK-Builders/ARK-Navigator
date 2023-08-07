package dev.arkbuilders.navigator.presentation

import android.app.Application
import android.os.StrictMode
import dev.arkbuilders.navigator.BuildConfig
import dev.arkbuilders.navigator.R
import dev.arkbuilders.navigator.di.AppComponent
import dev.arkbuilders.navigator.di.DaggerAppComponent
import dev.arkbuilders.navigator.data.preferences.PreferenceKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.acra.config.dialog
import org.acra.config.httpSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.acra.sender.HttpSender
import space.taran.arkfilepicker.folders.FoldersRepo
import space.taran.arklib.initArkLib
import space.taran.arklib.initRustLogger
import timber.log.Timber

class App : Application() {

    companion object {
        lateinit var instance: App
    }

    lateinit var appComponent: AppComponent
        private set

    init {
        if (BuildConfig.DEBUG) {
            StrictMode.enableDefaults()
        }
    }

    override fun onCreate() {
        super.onCreate()

        System.loadLibrary("arklib")
        FoldersRepo.init(this)
        initArkLib()
        initRustLogger()
        Timber.plant(Timber.DebugTree())

        instance = this

        appComponent = DaggerAppComponent
            .factory()
            .create(
                app = this,
                context = this,
                foldersRepo = FoldersRepo.instance
            )

        appComponent.arkBackup().backup()

        initAcra()
    }

    private fun initAcra() = CoroutineScope(Dispatchers.IO).launch {
        val enabled = appComponent.preferences().get(PreferenceKey.CrashReport)
        if (!enabled) return@launch

        initAcra {
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.JSON
            dialog {
                text = getString(R.string.crash_dialog_description)
                title = getString(R.string.crash_dialog_title)
                commentPrompt = getString(R.string.crash_dialog_comment)
            }
            httpSender {
                uri = BuildConfig.ACRA_URI
                basicAuthLogin = BuildConfig.ACRA_LOGIN
                basicAuthPassword = BuildConfig.ACRA_PASS
                httpMethod = HttpSender.Method.POST
            }
        }
    }
}
