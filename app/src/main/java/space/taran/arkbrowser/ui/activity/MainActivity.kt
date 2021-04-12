package space.taran.arkbrowser.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
import javax.inject.Inject


class MainActivity : MvpAppCompatActivity(), MainView {

    @Inject
    lateinit var navigatorHolder: NavigatorHolder

    @InjectPresenter
    lateinit var presenter: MainPresenter

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
        setSupportActionBar(toolbar)
        bottom_navigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.page_roots -> {
                    presenter.goToRootsScreen()
                    true
                }
                R.id.page_tags -> {
                    presenter.goToTagsScreen()
                    true
                }
                R.id.page_explorer -> {
                    presenter.goToExplorerScreen()
                    true
                }
                else -> true
            }
        }
    }

    override fun requestPerms() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        )
            presenter.permsGranted()
        else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                1
            )
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
                presenter.permsGranted()
            } else {
                presenter.permissionsDenied()
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun setTitle(title: String) {
        supportActionBar?.title = title
    }

    fun setToolbarVisibility(show: Boolean) {
        if (show)
            layout_toolbar.visibility = View.VISIBLE
        else
            layout_toolbar.visibility = View.GONE
    }

    fun setSelectedTab(pos: Int) {
        bottom_navigation.menu.getItem(pos).isChecked = true
    }

    override fun showToast(toast: String) {
        Toast.makeText(this, toast, Toast.LENGTH_LONG).show()
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

    override fun onBackPressed() {
        supportFragmentManager.fragments.forEach {
            if (it is BackButtonListener && it.backClicked()) {
                return
            }
        }
        presenter.backClicked()
    }
}
