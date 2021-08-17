package space.taran.arknavigator.di.modules

import space.taran.arknavigator.ui.App
import dagger.Module
import dagger.Provides
import space.taran.arknavigator.ui.resource.StringProvider
import javax.inject.Singleton

@Module
class AppModule(val app: App) {

    @Provides
    fun app(): App {
        return app
    }

    @Singleton
    @Provides
    fun stringProvider(app: App): StringProvider {
        return StringProvider(app)
    }
}