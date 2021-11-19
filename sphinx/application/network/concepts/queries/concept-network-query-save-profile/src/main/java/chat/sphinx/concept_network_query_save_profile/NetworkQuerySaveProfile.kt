package chat.sphinx.concept_network_query_save_profile

import chat.sphinx.concept_network_query_save_profile.model.SavePeopleProfileDto
import chat.sphinx.concept_network_query_save_profile.model.SaveProfileDto
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

abstract class NetworkQuerySaveProfile {

    abstract fun getPeopleProfileByKey(
        host: String,
        key: String
    ): Flow<LoadResponse<SaveProfileDto, ResponseError>>

    abstract fun savePeopleProfile(
        data: SavePeopleProfileDto,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<Any, ResponseError>>

    abstract fun deletePeopleProfile(): Flow<LoadResponse<Any, ResponseError>>
}