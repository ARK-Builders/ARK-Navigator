package space.taran.arkbrowser.mvp.presenter

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
    lateinit var roomRepo: RoomRepo

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.init()
        viewState.requestReadWritePerm()
        loadCardUris()
    }

    fun readWritePermGranted() {
        router.replaceScreen(Screens.HistoryScreen())
    }

    fun sdCardUriGranted(uri: String) {
        roomRepo.getCardUris().observeOn(AndroidSchedulers.mainThread()).subscribe(
            { list ->
               list.forEach {
                   if (it.uri == null) {
                       it.uri = uri
                       filesRepo.fileProvider.cardUris.add(it)
                       roomRepo.insertCardUri(it).subscribe()
                       router.replaceScreen(Screens.HistoryScreen())
                   }
               }
            }, {}
        )
    }

    fun permissionsDenied() {
        // TODO
    }

    private fun loadCardUris() {
        roomRepo.getCardUris().subscribe(
            { list ->
                filesRepo.fileProvider.cardUris = list.toMutableList()
            },
            {}
        )
    }
}