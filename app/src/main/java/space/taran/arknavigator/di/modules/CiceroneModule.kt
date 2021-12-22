package space.taran.arknavigator.di.modules

import dagger.Module
import dagger.Provides
import ru.terrakok.cicerone.Cicerone
import ru.terrakok.cicerone.NavigatorHolder
import space.taran.arknavigator.navigation.AppRouter
import javax.inject.Singleton

@Module
class CiceroneModule {

    @Singleton
    @Provides
    fun cicerone(): Cicerone<AppRouter> {
        return Cicerone.create(AppRouter())
    }

    @Provides
    fun navigationHolder(cicerone: Cicerone<AppRouter>): NavigatorHolder {
        return cicerone.navigatorHolder
    }

    @Provides
    fun router(cicerone: Cicerone<AppRouter>): AppRouter {
        return cicerone.router
    }
}