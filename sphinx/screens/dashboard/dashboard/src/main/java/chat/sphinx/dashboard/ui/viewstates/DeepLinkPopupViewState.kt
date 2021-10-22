package chat.sphinx.dashboard.ui.viewstates

import chat.sphinx.concept_network_query_verify_external.model.PersonInfoDto
import chat.sphinx.wrapper_common.ExternalAuthorizeLink
import chat.sphinx.wrapper_common.PeopleConnectLink
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class DeepLinkPopupViewState: ViewState<DeepLinkPopupViewState>() {

    object PopupDismissed: DeepLinkPopupViewState()

    object ExternalAuthorizePopupProcessing: DeepLinkPopupViewState()

    object PeopleConnectPopupLoadingPersonInfo: DeepLinkPopupViewState()
    object PeopleConnectPopupProcessing: DeepLinkPopupViewState()

    class ExternalAuthorizePopup(
        val link: ExternalAuthorizeLink
    ): DeepLinkPopupViewState()

    class PeopleConnectPopup(
        val alias: String,
        val description: String,
        val priceToMeet: Long,
        val photoUrl: String?,
        val personInfoDto: PersonInfoDto,
    ): DeepLinkPopupViewState()
}