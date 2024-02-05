package chat.sphinx.activitymain.di

import chat.sphinx.activitymain.fake.FakeUserColorHelper
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

@Module
@TestInstallIn(
    components = [ActivityComponent::class],
    replaces = [ActivityModule::class]
)
object FakeActivityModule {

    @Provides
    fun providesFakeUserColorHelper(
        fakeUserColorsHelper: FakeUserColorHelper
    ): UserColorsHelper = fakeUserColorsHelper

}