package chat.sphinx.feature_network_query_contact.model

import chat.sphinx.concept_network_query_contact.model.GetTribeMembersResponse
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeleteAccountRelayResponse(
    override val success: Boolean,
    override val response: Any?,
    override val error: String?
): RelayResponse<Any>()