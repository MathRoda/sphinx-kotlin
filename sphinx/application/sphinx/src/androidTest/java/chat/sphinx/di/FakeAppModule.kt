package chat.sphinx.di

import android.widget.ImageView
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.fake.FakeImageLoader
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AppModule::class]
)
object FakeAppModule {

    @Provides
    fun providesFakeImageLoader(
        fakeImageLoader: FakeImageLoader
    ): ImageLoader<ImageView> = fakeImageLoader
}