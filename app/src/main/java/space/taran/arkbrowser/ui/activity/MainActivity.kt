package space.taran.arkbrowser.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import space.taran.arkbrowser.R
import space.taran.arkbrowser.mvp.presenter.MainPresenter
import space.taran.arkbrowser.mvp.view.MainView
import space.taran.arkbrowser.ui.fragments.BackButtonListener
import kotlinx.android.synthetic.main.activity_main.*
import moxy.MvpAppCompatActivity
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import ru.terrakok.cicerone.NavigatorHolder
import ru.terrakok.cicerone.android.support.SupportAppNavigator
import space.taran.arkbrowser.ui.App
import java.lang.AssertionError
import javax.inject.Inject

class MainActivity : MvpAppCompatActivity(), MainView {

    @Inject
    lateinit var navigatorHolder: NavigatorHolder

    @InjectPresenter
    lateinit var presenter: MainPresenter

    private val navigator = SupportAppNavigator(this, R.id.container)

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("flow", "creating MainActivity")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        App.instance.appComponent.inject(this)
    }

    @ProvidePresenter
    fun providePresenter() = MainPresenter().apply {
        Log.d("flow", "creating MainPresenter")
        App.instance.appComponent.inject(this)
    }

    override fun init() {
        Log.d("flow", "initializing MainActivity")
        setSupportActionBar(toolbar)
        bottom_navigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.page_roots -> {
                    Log.d("flow", "switching to Roots screen")
                    presenter.goToRootsScreen()
                    true
                }
                R.id.page_tags -> {
                    Log.d("flow", "switching to Tags screen")
                    presenter.goToTagsScreen()
                    true
                }
                R.id.page_explorer -> {
                    Log.d("flow", "switching to Explorer screen")
                    presenter.goToExplorerScreen()
                    true
                }
                else -> {
                    Log.w("flow", "no handler found")
                    true
                }
            }
        }
    }

    override fun requestPerms() {
        //todo: are *_EXTERNAL_STORAGE permissions really necessary?
        val writePermission = ContextCompat.checkSelfPermission(this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val readPermission = ContextCompat.checkSelfPermission(this,
            Manifest.permission.READ_EXTERNAL_STORAGE)

        if (writePermission == PackageManager.PERMISSION_GRANTED &&
                readPermission == PackageManager.PERMISSION_GRANTED) {
            Log.d("permissions", "already granted")
            presenter.permsGranted()
        } else {
            val permissions = arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE)

            Log.d("permissions", "requesting $permissions")
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        granted: IntArray) {

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            Log.d("permissions", "granted $granted")

            if (granted.size == permissions.size) {
                val denied = permissions
                    .zip(granted.toList())
                    .filter { (_, result) -> result == PackageManager.PERMISSION_DENIED }
                    .map { (permission, _) -> permission }

                if (denied.isEmpty()) {
                    Log.d("permissions", "all necessary permissions granted")
                    presenter.permsGranted()
                } else {
                    Log.e("permissions", "denied $denied")
                    showToast("Permissions $denied denied")
                }
            } else {
                Log.e("permissions", "less permissions granted than expected")
                throw AssertionError("Failed to request permissions")
            }
        } else {
            Log.d("permissions", "unknown permissions result received")
        }

        super.onRequestPermissionsResult(requestCode, permissions, granted)
    }

    fun setTitle(title: String) {
        supportActionBar?.title = title
    }

    fun setToolbarVisibility(show: Boolean) {
        if (show) {
            layout_toolbar.visibility = View.VISIBLE
        } else {
            layout_toolbar.visibility = View.GONE
        }
    }

    fun setSelectedTab(pos: Int) {
        Log.d("flow", "tab $pos selected")
        bottom_navigation.menu.getItem(pos).isChecked = true
    }

    override fun showToast(toast: String) {
        Toast.makeText(this, toast, Toast.LENGTH_LONG).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == REQUEST_CODE_SD_CARD_URI) {
            Log.d("activity", "sdcard uri request resulted," +
                    "code $resultCode, intent: $intent")

            val treeUri = intent!!.data!!
            contentResolver.takePersistableUriPermission(treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

            //todo: is it really needed?
            presenter.sdCardUriGranted(treeUri.toString())
        } else {
            Log.d("activity", "unknown activity result received")
        }

        super.onActivityResult(requestCode, resultCode, intent)
    }

    override fun onResumeFragments() {
        Log.d("flow", "resuming fragments in MainActivity")
        super.onResumeFragments()
        navigatorHolder.setNavigator(navigator)
    }

    override fun onPause() {
        Log.d("flow", "pausing MainActivity")
        super.onPause()
        navigatorHolder.removeNavigator()
    }

    override fun onBackPressed() {
        Log.d("flow", "back pressed in MainActivity")
        supportFragmentManager.fragments.forEach {
            if (it is BackButtonListener && it.backClicked()) {
                return
            }
        }
        presenter.backClicked()
    }

    companion object {
        const val REQUEST_CODE_PERMISSIONS: Int = 1
        const val REQUEST_CODE_SD_CARD_URI: Int = 2
    }
}
