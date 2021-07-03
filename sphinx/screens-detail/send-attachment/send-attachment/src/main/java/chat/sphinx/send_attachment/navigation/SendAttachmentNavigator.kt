package chat.sphinx.send_attachment.navigation

import androidx.navigation.NavController
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class SendAttachmentNavigator(
    detailNavigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(detailNavigationDriver) {

    @JvmSynthetic
    internal suspend fun toSendAttachmentScreen(isConversation: Boolean) {
        navigationDriver.submitNavigationRequest(ToSendAttachmentDetail(isConversation))
    }

    @JvmSynthetic
    internal suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(PopBackStack())
    }
}