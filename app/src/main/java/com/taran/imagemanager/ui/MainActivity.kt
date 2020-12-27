package space.taran.arkbrowser.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.StrictMode
import androidx.core.app.ActivityCompat
import space.taran.arkbrowser.R
import space.taran.arkbrowser.mvp.model.entity.Folder
import space.taran.arkbrowser.mvp.presenter.MainPresenter
import space.taran.arkbrowser.mvp.view.MainView
import space.taran.arkbrowser.utils.*
import kotlinx.android.synthetic.main.activity_main.*
import moxy.MvpAppCompatActivity
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import ru.terrakok.cicerone.NavigatorHolder
import ru.terrakok.cicerone.android.support.SupportAppNavigator
import javax.inject.Inject


class MainActivity : MvpAppCompatActivity(), MainView {


    @Inject
    lateinit var navigatorHolder: NavigatorHolder

    @InjectPresenter
    lateinit var presenter: MainPresenter

    var sharedPref: SharedPreferences? = null

    val navigator = SupportAppNavigator(this, R.id.container)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        App.instance.appComponent.inject(this)
    }

    @ProvidePresenter
    fun providePresenter() = MainPresenter().apply {
        App.instance.appComponent.inject(this)
    }

    override fun init() {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        sharedPref = this.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun requestReadWritePerm() {
        val permGranted = sharedPref!!.getBoolean(PREF_READ_WRITE_GRANTED, false)

        if (!permGranted) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                1
            )
        } else {
            presenter.readWritePermGranted()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1) {
            if (grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                with(sharedPref!!.edit()) {
                    putBoolean(PREF_READ_WRITE_GRANTED, true)
                    apply()
                }
                presenter.readWritePermGranted()
            } else {
                presenter.permissionsDenied()
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun setTitle(title: String, isPath: Boolean) {
        if (isPath)
            toolbar_text.textSize = 16f
        else
            toolbar_text.textSize = 20f
        toolbar_text.text = title
        toolbar_text.isSelected = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 2) {
            val treeUri = data!!.data!!
            contentResolver
                .takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

            presenter.sdCardUriGranted(treeUri.toString())
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        navigatorHolder.setNavigator(navigator)
    }

    override fun onPause() {
        super.onPause()
        navigatorHolder.removeNavigator()
    }
}