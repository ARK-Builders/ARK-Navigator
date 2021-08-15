package space.taran.arknavigator.di.modules

import space.taran.arknavigator.ui.App
import dagger.Module
import dagger.Provides

@Module
class AppModule(val app: App) {

    @Provides
    fun app(): App {
        return app
    }

}