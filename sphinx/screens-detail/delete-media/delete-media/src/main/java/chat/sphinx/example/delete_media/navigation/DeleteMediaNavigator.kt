package chat.sphinx.example.delete_media.navigation

import androidx.navigation.NavController
import chat.sphinx.wrapper_common.dashboard.ChatId
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class DeleteMediaNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver) {
    @JvmSynthetic
    internal suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(PopBackStack())
    }

    abstract suspend fun toDeleteMediaDetail()

    abstract suspend fun closeDetailScreen()
}
