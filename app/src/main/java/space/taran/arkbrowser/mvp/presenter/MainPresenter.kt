package space.taran.arkbrowser.mvp.presenter

import space.taran.arkbrowser.mvp.model.repo.SynchronizeRepo
import space.taran.arkbrowser.mvp.model.repo.FilesRepo
import space.taran.arkbrowser.mvp.model.repo.RoomRepo
import space.taran.arkbrowser.mvp.view.MainView
import space.taran.arkbrowser.navigation.Screens
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import javax.inject.Inject

class MainPresenter: MvpPresenter<MainView>() {
    @Inject
    lateinit var filesRepo: FilesRepo

    @Inject
    lateinit var router: Router

    @Inject
    lateinit var syncRepo: SynchronizeRepo

    @Inject
    lateinit var roomRepo: RoomRepo

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.init()
        viewState.requestPerms()
    }

    fun permsGranted() {
        loadSdCardUris()
    }

    fun permissionsDenied() {

    }

    private fun loadSdCardUris() {
        roomRepo.getSdCardUris().subscribe(
            { list ->
                filesRepo.documentDataSource.sdCardUris = list.toMutableList()
                loadAndSyncRoots()
            },
            {}
        )
    }

    private fun loadAndSyncRoots() {
        roomRepo.getAllRoots().observeOn(AndroidSchedulers.mainThread()).subscribe(
            { list ->
                list.forEach { root ->
                    syncRepo.synchronizeRoot(root)
                }
                router.replaceScreen(Screens.RootScreen())
            },
            {}
        )
    }

    fun sdCardUriGranted(uri: String) {
        roomRepo.getSdCardUris().observeOn(AndroidSchedulers.mainThread()).subscribe(
            { list ->
               list.forEach {
                   if (it.uri == null) {
                       it.uri = uri
                       filesRepo.documentDataSource.sdCardUris.add(it)
                       roomRepo.insertSdCardUri(it).subscribe()
                   }
               }
            }, {}
        )
    }

    fun goToRootsScreen() {
        router.newRootScreen(Screens.RootScreen())
    }

    fun goToTagsScreen() {
        router.newRootScreen(Screens.TagsScreen(state = TagsPresenter.State.ALL_ROOTS))
    }

    fun goToExplorerScreen() {
        router.newRootScreen(Screens.ExplorerScreen())
    }

    fun backClicked() {
        router.exit()
    }

}
