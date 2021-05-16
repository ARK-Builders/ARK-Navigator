package space.taran.arkbrowser.mvp.presenter

import android.util.Log
import space.taran.arkbrowser.mvp.view.MainView
import space.taran.arkbrowser.navigation.Screens
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import space.taran.arkbrowser.utils.MAIN
import space.taran.arkbrowser.utils.PERMISSIONS
import space.taran.arkbrowser.utils.ROOT_PATH
import javax.inject.Inject

class MainPresenter: MvpPresenter<MainView>() {
    @Inject
    lateinit var router: Router

    override fun onFirstViewAttach() {
        Log.d(MAIN, "first view attached in MainPresenter")
        super.onFirstViewAttach()
        viewState.init()
        viewState.requestPerms()
    }

    fun permsGranted() {
        Log.d(MAIN, "creating Roots screen")
        //todo: default to TagsScreen if there are any roots added
        router.replaceScreen(Screens.RootsScreen())
    }

    fun sdCardUriGranted(uri: String) {
        Log.d(PERMISSIONS, "[mock] sdcard uri granted for $uri")
        //todo
    }

    fun goToRootsScreen() {
        Log.d(MAIN, "creating Roots screen")
        router.newRootScreen(Screens.RootsScreen())
    }

    fun goToTagsScreen() {
        Log.d(MAIN, "[mock] creating Tags screen")
        router.newRootScreen(Screens.TagsScreen(null, null))
    }

    fun backClicked() {
        Log.d(MAIN, "back clicked in MainPresenter")
        router.exit()
    }
}
