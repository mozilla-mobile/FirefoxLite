package org.mozilla.rocket.dagger

import android.content.Context
import dagger.Binds
import dagger.Component
import dagger.Module
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import org.mozilla.focus.FocusApplication
import javax.inject.Qualifier
import javax.inject.Singleton

@Component(
    modules = [AppModule::class]
)
interface AppComponent : AndroidInjector<FocusApplication> {

    @Component.Builder
    abstract class Builder : AndroidInjector.Builder<FocusApplication>()
}

@Module(includes = [AndroidSupportInjectionModule::class])
abstract class AppModule {

    @Singleton
    @Binds
    @AppContext
    abstract fun provideContext(app: FocusApplication): Context
}

@Qualifier
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class AppContext
