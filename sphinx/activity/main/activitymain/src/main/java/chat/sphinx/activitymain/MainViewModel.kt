package chat.sphinx.activitymain

import android.net.Uri
import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.activitymain.navigation.drivers.AuthenticationNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.activitymain.ui.MainViewState
import chat.sphinx.concept_network_query_chat.model.feed.FeedItemDto
import chat.sphinx.concept_repository_actions.ActionsRepository
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.dashboard.navigation.ToDashboardScreen
import chat.sphinx.dashboard.ui.getMediaDuration
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_feed.FeedItemDuration
import chat.sphinx.wrapper_podcast.PodcastEpisode
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_activity.NavigationViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_authentication.coordinator.AuthenticationCoordinator
import io.matthewnelson.concept_authentication.coordinator.AuthenticationRequest
import io.matthewnelson.concept_authentication.state.AuthenticationState
import io.matthewnelson.concept_authentication.state.AuthenticationStateManager
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

@HiltViewModel
internal class MainViewModel @Inject constructor(
    private val authenticationCoordinator: AuthenticationCoordinator,
    private val authenticationStateManager: AuthenticationStateManager,
    val authenticationDriver: AuthenticationNavigationDriver,
    val detailDriver: DetailNavigationDriver,
    dispatchers: CoroutineDispatchers,
    override val navigationDriver: PrimaryNavigationDriver,
    private val actionsRepository: ActionsRepository,
    private val feedRepository: FeedRepository,
    private val mediaPlayerServiceController: MediaPlayerServiceController,
): BaseViewModel<MainViewState>(dispatchers, MainViewState.DetailScreenInactive), NavigationViewModel<PrimaryNavigationDriver>
{
    init {
        viewModelScope.launch(mainImmediate) {
            authenticationStateManager.authenticationStateFlow.collect { state ->
                @Exhaustive
                when (state) {
                    is AuthenticationState.NotRequired -> {
                        // Do nothing
                    }
                    is AuthenticationState.Required.InitialLogIn -> {
                        // Handled by the Splash Screen
                    }
                    is AuthenticationState.Required.LoggedOut -> {
                        // Blow it up
                        authenticationCoordinator.submitAuthenticationRequest(
                            AuthenticationRequest.LogIn(privateKey = null)
                        )
                    }
                }
            }
        }
    }

    suspend fun handleDeepLink(deepLink: String) {
        if (authenticationStateManager.authenticationStateFlow.value == AuthenticationState.NotRequired) {
            navigationDriver.submitNavigationRequest(
                ToDashboardScreen(
                    popUpToId = R.id.main_primary_nav_graph,
                    updateBackgroundLoginTime = false,
                    deepLink = deepLink
                )
            )
        }
    }

    fun syncActions() {
        actionsRepository.syncActions()
    }

    fun restoreContentFeedStatuses() {
        val playingContent = mediaPlayerServiceController.getPlayingContent()

        feedRepository.restoreContentFeedStatuses(
            playingContent?.first,
            playingContent?.second,
            ::retrieveEpisodeDuration
        )
    }

    fun saveContentFeedStatuses() {
        feedRepository.saveContentFeedStatuses()
    }

    private fun retrieveEpisodeDuration(
        url: String
    ) : Long {
        return Uri.parse(url).getMediaDuration(false)
    }

}
