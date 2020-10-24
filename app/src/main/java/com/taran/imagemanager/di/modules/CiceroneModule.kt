package com.taran.imagemanager.di.modules

import dagger.Module
import dagger.Provides
import ru.terrakok.cicerone.Cicerone
import ru.terrakok.cicerone.NavigatorHolder
import ru.terrakok.cicerone.Router
import javax.inject.Singleton

@Module
class CiceroneModule {

    @Singleton
    @Provides
    fun cicerone(): Cicerone<Router> {
        return Cicerone.create()
    }

    @Provides
    fun navigationHolder(cicerone: Cicerone<Router>): NavigatorHolder {
        return cicerone.navigatorHolder
    }

    @Provides
    fun router(cicerone: Cicerone<Router>): Router {
        return cicerone.router
    }
}