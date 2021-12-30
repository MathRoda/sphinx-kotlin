package chat.sphinx.video_screen.ui.watch

import android.net.Uri
import android.widget.VideoView
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.video_player_controller.VideoPlayerController
import chat.sphinx.video_screen.ui.VideoFeedScreenViewModel
import chat.sphinx.video_screen.ui.viewstate.LoadingVideoViewState
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.launch
import javax.inject.Inject

internal inline val VideoFeedWatchScreenFragmentArgs.chatId: ChatId
    get() = ChatId(argChatId)

internal inline val VideoFeedWatchScreenFragmentArgs.feedUrl: FeedUrl?
    get() = FeedUrl(argFeedUrl)

internal inline val VideoFeedWatchScreenFragmentArgs.feedId: FeedId?
    get() = FeedId(argFeedId)

@HiltViewModel
internal class VideoFeedWatchScreenViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val repositoryMedia: RepositoryMedia,
    feedRepository: FeedRepository,
): VideoFeedScreenViewModel(
    dispatchers,
    chatRepository,
    repositoryMedia,
    feedRepository
)
{
    private val args: VideoFeedWatchScreenFragmentArgs by savedStateHandle.navArgs()

    init {
        subscribeToViewStateFlow()

        viewModelScope.launch(mainImmediate) {
            chatRepository.updateChatContentSeenAt(
                getArgChatId()
            )
        }
    }

    open val loadingVideoStateContainer: ViewStateContainer<LoadingVideoViewState> by lazy {
        ViewStateContainer(LoadingVideoViewState.Idle)
    }

    private val videoPlayerController: VideoPlayerController by lazy {
        VideoPlayerController(
            viewModelScope = viewModelScope,
            updateIsPlaying = { },
            updateMetaDataCallback = { _, _, _ ->
                loadingVideoStateContainer.updateViewState(
                    LoadingVideoViewState.MetaDataLoaded
                )
            },
            updateCurrentTimeCallback = { },
            completePlaybackCallback = { },
            dispatchers
        )
    }

    fun initializeVideo(
        videoUri: Uri,
        videoDuration: Int?
    ) {
        if (videoDuration != null && videoDuration > 0) {
            videoPlayerController.initializeVideo(
                videoUri,
                videoDuration * 1000
            )
        } else {
            videoPlayerController.initializeVideo(videoUri)
        }
    }

    fun setVideoView(videoView: VideoView) {
        videoPlayerController.setVideo(videoView)
    }

    override fun getArgChatId(): ChatId {
        return args.chatId
    }

    override fun getArgFeedUrl(): FeedUrl? {
        return args.feedUrl
    }

    override fun getArgFeedId(): FeedId? {
        return args.feedId
    }
}