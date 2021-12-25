package space.taran.arknavigator.mvp.presenter

import android.util.Log
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import space.taran.arknavigator.mvp.view.MainView
import space.taran.arknavigator.navigation.Screens
import space.taran.arknavigator.utils.MAIN
import javax.inject.Inject

class MainPresenter : MvpPresenter<MainView>() {
    @Inject
    lateinit var router: Router

    override fun onFirstViewAttach() {
        Log.d(MAIN, "first view attached in MainPresenter")
        super.onFirstViewAttach()
        viewState.init()
        viewState.requestPerms()
    }

    fun permsGranted() {
        Log.d(MAIN, "creating Folders screen")
        router.replaceScreen(Screens.FoldersScreen())
    }

    fun goToFoldersScreen() {
        Log.d(MAIN, "creating Folders screen")
        router.newRootScreen(Screens.FoldersScreen())
    }

    fun goToResourcesScreen() {
        Log.d(MAIN, "creating Resources screen")
        router.newRootScreen(Screens.ResourcesScreen(null, null))
    }

    fun backClicked() {
        Log.d(MAIN, "[back] clicked in MainPresenter")
        router.exit()
    }
}
