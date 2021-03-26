package space.taran.arkbrowser.di.modules

import space.taran.arkbrowser.ui.App
import dagger.Module
import dagger.Provides

@Module
class AppModule(val app: App) {

    @Provides
    fun app(): App {
        return app
    }

}