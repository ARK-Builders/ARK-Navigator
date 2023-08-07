package dev.arkbuilders.navigator.presentation.screen.main

import android.util.Log
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import space.taran.arkfilepicker.folders.FoldersRepo
import space.taran.arkfilepicker.folders.RootAndFav
import dev.arkbuilders.navigator.presentation.navigation.AppRouter
import dev.arkbuilders.navigator.presentation.navigation.Screens
import dev.arkbuilders.navigator.data.utils.LogTags.MAIN
import javax.inject.Inject

class MainPresenter : MvpPresenter<MainView>() {
    @Inject
    lateinit var router: AppRouter

    @Inject
    lateinit var folders: FoldersRepo

    override fun onFirstViewAttach() {
        Log.d(MAIN, "first view attached in MainPresenter")
        super.onFirstViewAttach()
        viewState.init()
        viewState.requestPerms()
    }

    fun permsGranted(isActiveScreenExist: Boolean) {
        if (!isActiveScreenExist) {
            Log.d(MAIN, "creating Folders screen")
            router.replaceScreen(Screens.FoldersScreen())
        }
    }

    fun goToFoldersScreen() {
        Log.d(MAIN, "creating Folders screen")
        router.newRootScreen(Screens.FoldersScreen())
    }

    fun goToResourcesScreen() {
        Log.d(MAIN, "creating Resources screen")
        presenterScope.launch {
            val folders = folders.provideFolders()
            if (folders.size < 1) {
                viewState.enterResourceFragmentFailed()
            } else {
                Log.d(MAIN, "switching to Resources screen")
                router.newRootScreen(Screens.ResourcesScreen(RootAndFav(null, null)))
            }
            true
        }
    }

    fun goToSettingsScreen() {
        Log.d(MAIN, "creating Settings screen")
        router.newRootScreen(Screens.SettingsScreen())
    }

    fun backClicked() {
        Log.d(MAIN, "[back] clicked in MainPresenter")
        router.exit()
    }
}
