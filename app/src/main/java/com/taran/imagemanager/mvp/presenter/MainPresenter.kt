package com.taran.imagemanager.mvp.presenter

import com.taran.imagemanager.mvp.model.repo.FilesRepo
import com.taran.imagemanager.mvp.model.repo.RoomRepo
import com.taran.imagemanager.mvp.view.MainView
import com.taran.imagemanager.navigation.Screens
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