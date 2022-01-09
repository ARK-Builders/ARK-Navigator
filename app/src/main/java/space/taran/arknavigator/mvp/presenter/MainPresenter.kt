package space.taran.arknavigator.mvp.presenter

import android.util.Log
import moxy.MvpPresenter
import space.taran.arknavigator.mvp.model.repo.RootAndFav
import space.taran.arknavigator.mvp.view.MainView
import space.taran.arknavigator.navigation.AppRouter
import space.taran.arknavigator.navigation.Screens
import space.taran.arknavigator.utils.MAIN
import javax.inject.Inject

class MainPresenter: MvpPresenter<MainView>() {
    @Inject
    lateinit var router: AppRouter

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
        router.newRootScreen(Screens.ResourcesScreen(RootAndFav(null, null)))
    }
}
