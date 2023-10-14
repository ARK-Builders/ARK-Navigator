package dev.arkbuilders.navigator.presentation.screen.main

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import dev.arkbuilders.arkfilepicker.folders.FoldersRepo
import dev.arkbuilders.arkfilepicker.folders.RootAndFav
import dev.arkbuilders.navigator.R
import dev.arkbuilders.navigator.data.PermissionsHelper
import dev.arkbuilders.navigator.data.utils.LogTags.MAIN
import dev.arkbuilders.navigator.databinding.ActivityMainBinding
import dev.arkbuilders.navigator.presentation.App
import dev.arkbuilders.navigator.presentation.navigation.AppNavigator
import dev.arkbuilders.navigator.presentation.navigation.AppRouter
import dev.arkbuilders.navigator.presentation.navigation.Screens
import dev.arkbuilders.navigator.presentation.utils.toast
import kotlinx.coroutines.launch
import ru.terrakok.cicerone.NavigatorHolder
import javax.inject.Inject

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    @Inject
    lateinit var navigatorHolder: NavigatorHolder

    @Inject
    lateinit var foldersRepo: FoldersRepo

    @Inject
    lateinit var router: AppRouter

    @Inject
    lateinit var permsHelper: PermissionsHelper

    private val binding by viewBinding(ActivityMainBinding::bind)

    private val navigator = AppNavigator(this, R.id.container)

    override fun onCreate(savedInstanceState: Bundle?) {
        App.instance.appComponent.inject(this)
        super.onCreate(savedInstanceState)
        permsHelper.registerActivity(this)
        if (savedInstanceState == null)
            router.replaceScreen(Screens.FoldersScreen())
        init()
    }

    fun init() {
        Log.d(MAIN, "initializing")
        binding.bottomNavigation.setOnApplyWindowInsetsListener(null)
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.page_settings -> {
                    Log.d(MAIN, "switching to Settings screen")
                    router.newRootScreen(Screens.SettingsScreen())
                    true
                }

                R.id.page_roots -> {
                    Log.d(MAIN, "switching to Folders screen")
                    router.replaceScreen(Screens.FoldersScreen())
                    true
                }

                R.id.page_tags -> {
                    Log.d(MAIN, "switching to Resources screen")
                    lifecycleScope.launch {
                        val folders = foldersRepo.provideFolders()
                        if (folders.isEmpty()) {
                            enterResourceFragmentFailed()
                        } else {
                            Log.d(MAIN, "switching to Resources screen")
                            router.newRootScreen(
                                Screens.ResourcesScreen(
                                    RootAndFav(
                                        null,
                                        null
                                    )
                                )
                            )
                        }
                        true
                    }
                    true
                }

                else -> {
                    Log.w(MAIN, "no handler found")
                    true
                }
            }
        }

        binding.bottomNavigation.setOnItemReselectedListener {}
    }

    private fun enterResourceFragmentFailed() {
        toast(R.string.toast_add_paths)
        binding.bottomNavigation.selectedItemId = R.id.page_roots
    }

    fun setBottomNavigationVisibility(isVisible: Boolean) {
        binding.bottomNavigation.isVisible = isVisible
    }

    fun setSelectedTab(menuItemID: Int) {
        binding.bottomNavigation.apply {
            Log.d(
                MAIN,
                "tab with id $menuItemID selected," +
                    "title: ${menu.findItem(menuItemID).title}"
            )
            menu.findItem(menuItemID).isChecked = true
        }
    }

    fun setBottomNavigationEnabled(isEnabled: Boolean) {
        binding.bottomNavigation.menu.forEach { item ->
            item.isEnabled = isEnabled
        }
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
}
