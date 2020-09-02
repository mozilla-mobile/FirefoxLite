package org.mozilla.rocket.content.di

import dagger.Module
import dagger.Provides
import org.mozilla.rocket.content.common.ui.ContentTabBottomBarViewModel
import org.mozilla.rocket.content.common.ui.ContentTabTelemetryViewModel
import org.mozilla.rocket.content.common.ui.TabSwipeTelemetryViewModel
import org.mozilla.rocket.content.common.ui.VerticalTelemetryViewModel
import org.mozilla.rocket.content.news.di.NewsModule

@Module(includes = [NewsModule::class])
object ContentModule {

    @JvmStatic
    @Provides
    fun provideVerticalTelemetryViewModel(): VerticalTelemetryViewModel = VerticalTelemetryViewModel()

    @JvmStatic
    @Provides
    fun provideContentTabBottomBarViewModel(): ContentTabBottomBarViewModel = ContentTabBottomBarViewModel()

    @JvmStatic
    @Provides
    fun provideContentTabTelemetryViewModel(): ContentTabTelemetryViewModel = ContentTabTelemetryViewModel()

    @JvmStatic
    @Provides
    fun provideTabSwipeTelemetryViewModel(): TabSwipeTelemetryViewModel = TabSwipeTelemetryViewModel()
}
