package space.taran.arknavigator.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.core.view.isVisible
import space.taran.arknavigator.R
import space.taran.arknavigator.mvp.presenter.MainPresenter
import space.taran.arknavigator.mvp.view.MainView
import space.taran.arknavigator.ui.fragments.BackButtonListener
import moxy.MvpAppCompatActivity
import moxy.ktx.moxyPresenter
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import ru.terrakok.cicerone.NavigatorHolder
import ru.terrakok.cicerone.android.support.SupportAppNavigator
import space.taran.arknavigator.databinding.ActivityMainBinding
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.ui.fragments.utils.Notifications
import space.taran.arknavigator.utils.MAIN
import space.taran.arknavigator.utils.PERMISSIONS
import java.lang.AssertionError
import javax.inject.Inject

class MainActivity : MvpAppCompatActivity(), MainView {

    @Inject
    lateinit var navigatorHolder: NavigatorHolder

    private lateinit var binding: ActivityMainBinding

    private val presenter by moxyPresenter {
        MainPresenter().apply {
            Log.d(MAIN, "creating MainPresenter")
            App.instance.appComponent.inject(this)
        }
    }

    private val navigator = SupportAppNavigator(this, R.id.container)

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(MAIN, "creating")
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        App.instance.appComponent.inject(this)
    }

    override fun init() {
        Log.d(MAIN, "initializing")
        setSupportActionBar(binding.toolbar)
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.page_roots -> {
                    Log.d(MAIN, "switching to Folders screen")
                    presenter.goToFoldersScreen()
                    true
                }
                R.id.page_tags -> {
                    Log.d(MAIN, "switching to Resources screen")
                    presenter.goToResourcesScreen()
                    true
                }
                else -> {
                    Log.w(MAIN, "no handler found")
                    true
                }
            }
        }
        binding.bottomNavigation.setOnItemReselectedListener{}
    }

    override fun requestPerms() {
        //todo: are *_EXTERNAL_STORAGE permissions really necessary?
        val writePermission = ContextCompat.checkSelfPermission(this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val readPermission = ContextCompat.checkSelfPermission(this,
            Manifest.permission.READ_EXTERNAL_STORAGE)

        if (writePermission == PackageManager.PERMISSION_GRANTED &&
                readPermission == PackageManager.PERMISSION_GRANTED) {
            Log.d(PERMISSIONS, "already granted")
            presenter.permsGranted()
        } else {
            val permissions = arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE)

            Log.d(PERMISSIONS, "requesting $permissions")
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        granted: IntArray) {

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            Log.d(PERMISSIONS, "granted $granted")

            if (granted.size == permissions.size) {
                val denied = permissions
                    .zip(granted.toList())
                    .filter { (_, result) -> result == PackageManager.PERMISSION_DENIED }
                    .map { (permission, _) -> permission }

                if (denied.isEmpty()) {
                    Log.d(PERMISSIONS, "all necessary permissions granted")
                    presenter.permsGranted()
                } else {
                    Log.e(PERMISSIONS, "denied $denied")
                    notifyUser("Permissions $denied denied")
                }
            } else {
                Log.e(PERMISSIONS, "less permissions granted than expected")
                throw AssertionError("Failed to request permissions")
            }
        } else {
            Log.d(PERMISSIONS, "unknown permissions result received")
        }

        super.onRequestPermissionsResult(requestCode, permissions, granted)
    }

    fun setBottomNavigationVisibility(isVisible: Boolean) {
        binding.bottomNavigation.isVisible = isVisible
    }

    fun setTitle(title: String) {
        supportActionBar?.title = title
    }

    fun setToolbarVisibility(show: Boolean) {
        if (show) {
            binding.layoutToolbar.visibility = View.VISIBLE
        } else {
            binding.layoutToolbar.visibility = View.GONE
        }
    }

    fun setSelectedTab(pos: Int) {
        Log.d(MAIN, "tab $pos selected")
        binding.bottomNavigation.menu.getItem(pos).isChecked = true
    }

    fun setBottomNavigationEnabled(isEnabled: Boolean) {
        binding.bottomNavigation.menu.forEach { item ->
            item.isEnabled = isEnabled
        }
    }

    override fun notifyUser(message: String, moreTime: Boolean) {
        Notifications.notifyUser(this, message, moreTime)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == REQUEST_CODE_SD_CARD_URI) {
            Log.d(MAIN, "sdcard uri request resulted," +
                    "code $resultCode, intent: $intent")

            val treeUri = intent!!.data!!
            contentResolver.takePersistableUriPermission(treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        } else {
            Log.d(MAIN, "unknown activity result received")
        }

        super.onActivityResult(requestCode, resultCode, intent)
    }

    override fun onResumeFragments() {
        Log.d(MAIN, "resuming fragments in MainActivity")
        super.onResumeFragments()
        navigatorHolder.setNavigator(navigator)
    }

    override fun onPause() {
        Log.d(MAIN, "pausing MainActivity")
        super.onPause()
        navigatorHolder.removeNavigator()
    }

    override fun onBackPressed() {
        Log.d(MAIN, "back pressed in MainActivity")
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
