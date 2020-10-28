package com.taran.imagemanager.mvp.presenter

import com.taran.imagemanager.mvp.view.MainView
import com.taran.imagemanager.navigation.Screens
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import javax.inject.Inject

class MainPresenter: MvpPresenter<MainView>() {
    @Inject
    lateinit var router: Router

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.init()
        viewState.requestPermissions()
    }

    fun permissionsGranted() {
        router.replaceScreen(Screens.HistoryScreen())
    }

    fun permissionsDenied() {
        // TODO
    }
}