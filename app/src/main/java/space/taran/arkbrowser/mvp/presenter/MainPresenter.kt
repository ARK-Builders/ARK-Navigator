package space.taran.arkbrowser.mvp.presenter

import android.util.Log
import space.taran.arkbrowser.mvp.view.MainView
import space.taran.arkbrowser.navigation.Screens
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import javax.inject.Inject

class MainPresenter: MvpPresenter<MainView>() {
    @Inject
    lateinit var router: Router

    override fun onFirstViewAttach() {
        Log.d("flow", "first view attached in MainPresenter")
        super.onFirstViewAttach()
        viewState.init()
        viewState.requestPerms()
    }

    fun permsGranted() {
        loadSdCardUris()
    }

    private fun loadSdCardUris() {
        Log.d("flow", "[mock] loading sdcard URIs")
        Log.d("flow", "creating Roots screen")
        router.replaceScreen(Screens.RootsScreen())
    }

    fun sdCardUriGranted(uri: String) {
        Log.d("activity", "[mock] sdcard uri granted for $uri")
        //todo
    }

    fun goToRootsScreen() {
        Log.d("flow", "creating Roots screen")
        router.newRootScreen(Screens.RootsScreen())
    }

    fun goToTagsScreen() {
        Log.d("flow", "[mock] creating Tags screen")
//        router.newRootScreen(Screens.TagsScreen(
//            resources = rootsRepo.roots.values.flatMap { it.resources }))
    }

    fun goToExplorerScreen() {
        Log.d("flow", "creating Explorer screen")
        router.newRootScreen(Screens.ExplorerScreen())
    }

    fun backClicked() {
        Log.d("flow", "back clicked in MainPresenter")
        router.exit()
    }
}
