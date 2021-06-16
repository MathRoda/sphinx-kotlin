package chat.sphinx.feature_network_query_invite

import chat.sphinx.concept_network_query_invite.NetworkQueryInvite
import chat.sphinx.concept_network_query_invite.model.HubRedeemInviteResponse
import chat.sphinx.concept_network_query_invite.model.RedeemInviteResponse
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_invite.model.RedeemInviteRelayResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

class NetworkQueryInviteImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQueryInvite() {

    companion object {
        private const val ENDPOINT_INVITES = "/invites"
        private const val ENDPOINT_SIGNUP = "/api/v1/signup"
        private const val ENDPOINT_SIGNUP_FINISH = "/invites/finish"
        private const val HUB_URL = "https://hub.sphinx.chat"
    }

    override fun redeemInvite(
        inviteString: String
    ): Flow<LoadResponse<HubRedeemInviteResponse, ResponseError>> {
        return networkRelayCall.post(
            url = HUB_URL + ENDPOINT_SIGNUP,
            responseJsonClass = HubRedeemInviteResponse::class.java,
            requestBodyJsonClass = Map::class.java,
            requestBody = mapOf(
                Pair("invite_string", inviteString),
            )
        )
    }

    override fun finishInvite(
        relayUrl: RelayUrl,
        inviteString: String
    ): Flow<LoadResponse<RedeemInviteResponse, ResponseError>> {
        return networkRelayCall.relayUnauthenticatedPost(
            responseJsonClass = RedeemInviteRelayResponse::class.java,
            relayEndpoint = ENDPOINT_SIGNUP_FINISH,
            requestBodyJsonClass = Map::class.java,
            requestBody = mapOf(
                Pair("invite_string", inviteString),
            ),
            relayUrl = relayUrl
        )
    }

    ///////////
    /// GET ///
    ///////////

    ///////////
    /// PUT ///
    ///////////

    ////////////
    /// POST ///
    ////////////
//    app.post('/invites', invites.createInvite)
//    app.post('/invites/:invite_string/pay', invites.payInvite)
//    app.post('/invites/finish', invites.finishInvite)

    //////////////
    /// DELETE ///
    //////////////
}