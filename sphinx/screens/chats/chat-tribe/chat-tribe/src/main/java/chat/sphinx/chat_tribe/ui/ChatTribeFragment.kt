package chat.sphinx.chat_tribe.ui

import android.animation.Animator
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.chat_common.databinding.*
import chat.sphinx.chat_common.ui.ChatFragment
import chat.sphinx.chat_common.ui.viewstate.messagereply.MessageReplyViewState
import chat.sphinx.chat_tribe.R
import chat.sphinx.chat_tribe.databinding.FragmentChatTribeBinding
import chat.sphinx.chat_tribe.databinding.LayoutChatTribePopupBinding
import chat.sphinx.chat_tribe.model.TribeFeedData
import chat.sphinx.chat_tribe.ui.viewstate.BoostAnimationViewState
import chat.sphinx.chat_tribe.ui.viewstate.TribePopupViewState
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.menu_bottom.databinding.LayoutMenuBottomBinding
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.resources.databinding.LayoutBoostFireworksBinding
import chat.sphinx.resources.databinding.LayoutPodcastPlayerFooterBinding
import chat.sphinx.resources.getRandomHexCode
import chat.sphinx.resources.setBackgroundRandomColor
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.util.getInitials
import chat.sphinx.wrapper_message.*
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.collect
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class ChatTribeFragment: ChatFragment<
        FragmentChatTribeBinding,
        ChatTribeFragmentArgs,
        ChatTribeViewModel,
        >(R.layout.fragment_chat_tribe)
{
    override val binding: FragmentChatTribeBinding by viewBinding(FragmentChatTribeBinding::bind)
    private val podcastPlayerBinding: LayoutPodcastPlayerFooterBinding
        get() = binding.includePodcastPlayerFooter
    private val boostAnimationBinding: LayoutBoostFireworksBinding
        get() = binding.includeLayoutBoostFireworks
    private val tribePopupBinding: LayoutChatTribePopupBinding
        get() = binding.includeLayoutPopup

    override val footerBinding: LayoutChatFooterBinding
        get() = binding.includeChatTribeFooter
    override val recordingCircleBinding: LayoutChatRecordingCircleBinding
        get() = binding.includeChatRecordingCircle
    override val headerBinding: LayoutChatHeaderBinding
        get() = binding.includeChatTribeHeader
    override val replyingMessageBinding: LayoutMessageReplyBinding
        get() = binding.includeChatTribeMessageReply
    override val selectedMessageBinding: LayoutSelectedMessageBinding
        get() = binding.includeChatTribeSelectedMessage
    override val selectedMessageHolderBinding: LayoutMessageHolderBinding
        get() = binding.includeChatTribeSelectedMessage.includeLayoutMessageHolderSelectedMessage
    override val attachmentSendBinding: LayoutAttachmentSendPreviewBinding
        get() = binding.includeChatTribeAttachmentSendPreview
    override val menuBinding: LayoutChatMenuBinding
        get() = binding.includeChatTribeMenu
    override val callMenuBinding: LayoutMenuBottomBinding
        get() = binding.includeLayoutMenuBottomCall
    override val attachmentFullscreenBinding: LayoutAttachmentFullscreenBinding
        get() = binding.includeChatTribeAttachmentFullscreen

    override val menuEnablePayments: Boolean
        get() = false

    override val recyclerView: RecyclerView
        get() = binding.recyclerViewMessages

    override val viewModel: ChatTribeViewModel by viewModels()
    private val tribeFeedViewModel: TribeFeedViewModel by viewModels()

    @Inject
    @Suppress("ProtectedInFinal", "PropertyName")
    protected lateinit var _userColorsHelper: UserColorsHelper
    override val userColorsHelper: UserColorsHelper
        get() = _userColorsHelper

    @Inject
    @Suppress("ProtectedInFinal", "PropertyName")
    protected lateinit var _imageLoader: ImageLoader<ImageView>
    override val imageLoader: ImageLoader<ImageView>
        get() = _imageLoader

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        BackPressHandler(viewLifecycleOwner, requireActivity())

        lifecycleScope.launch(viewModel.mainImmediate) {
            try {
                viewModel.feedDataStateFlow.collect { data ->
                    @Exhaustive
                    when (data) {
                        is TribeFeedData.Loading -> {}
                        is TribeFeedData.Result -> {
                            tribeFeedViewModel.init(data)
                            throw Exception()
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        podcastPlayerBinding.apply {
            imageViewForward30Button.setOnClickListener {
                tribeFeedViewModel.podcastViewStateContainer.value.clickFastForward?.invoke()
            }
            textViewPlayButton.setOnClickListener {
                tribeFeedViewModel.podcastViewStateContainer.value.clickPlayPause?.invoke()
            }
            animationViewPauseButton.setOnClickListener {
                tribeFeedViewModel.podcastViewStateContainer.value.clickPlayPause?.invoke()
            }
            layoutConstraintPodcastInfo.setOnClickListener {
                tribeFeedViewModel.podcastViewStateContainer.value.clickTitle?.invoke()
            }
        }

        boostAnimationBinding.lottieAnimationView.addAnimatorListener(object : Animator.AnimatorListener{
            override fun onAnimationEnd(animation: Animator?) {
                boostAnimationBinding.root.gone
            }

            override fun onAnimationRepeat(animation: Animator?) {}

            override fun onAnimationCancel(animation: Animator?) {}

            override fun onAnimationStart(animation: Animator?) {}
        })

        tribeFeedViewModel.shareClipHandler = { podcastClip ->
            viewModel.messageReplyViewStateContainer.updateViewState(
                MessageReplyViewState.CommentingOnPodcast(podcastClip)
            )
        }

        tribePopupBinding.layoutChatTribePopup.apply {
            buttonSendSats.setOnClickListener {
                viewModel.goToPaymentSend()
            }

            textViewDirectPaymentPopupClose.setOnClickListener {
                viewModel.tribePopupViewStateContainer.updateViewState(TribePopupViewState.Idle)
            }
        }
    }

    private inner class BackPressHandler(
        owner: LifecycleOwner,
        activity: FragmentActivity,
    ): OnBackPressedCallback(true) {

        init {
            activity.apply {
                onBackPressedDispatcher.addCallback(
                    owner,
                    this@BackPressHandler,
                )
            }
        }

        override fun handleOnBackPressed() {
            if (viewModel.tribePopupViewStateContainer.value is TribePopupViewState.TribeMemberPopup) {
                viewModel.tribePopupViewStateContainer.updateViewState(TribePopupViewState.Idle)
            } else {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.handleCommonChatOnBackPressed()
                }
            }
        }
    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            tribeFeedViewModel.boostAnimationViewStateContainer.collect { viewState ->
                @Exhaustive
                when (viewState) {
                    is BoostAnimationViewState.Idle -> {}

                    is BoostAnimationViewState.BoosAnimationInfo -> {
                        boostAnimationBinding.apply {

                            viewState.photoUrl?.let { photoUrl ->
                                imageLoader.load(
                                    imageViewProfilePicture,
                                    photoUrl.value,
                                    ImageLoaderOptions.Builder()
                                        .placeholderResId(chat.sphinx.podcast_player.R.drawable.ic_profile_avatar_circle)
                                        .transformation(Transformation.CircleCrop)
                                        .build()
                                )
                            }

                            textViewSatsAmount.text = viewState.amount?.asFormattedString()
                        }
                    }
                }
            }
        }

        // TODO: Remove hackery (utilized now to update podcast object's sats per minute
        //  value if it's changed from tribe detail screen)
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            tribeFeedViewModel.satsPerMinuteStateFlow.collect {
                /* no-op */
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            tribeFeedViewModel.podcastViewStateContainer.collect { viewState ->
                podcastPlayerBinding.apply {
                    when (viewState) {
                        is PodcastViewState.NoPodcast -> {
                            root.gone
                        }
                        is PodcastViewState.PodcastVS -> {

                            textViewPlayButton.goneIfFalse(viewState.showPlayButton && !viewState.showLoading)
                            animationViewPauseButton.goneIfFalse(!viewState.showPlayButton && !viewState.showLoading)

                            progressBar.progress = viewState.playingProgress

                            textViewEpisodeTitle.isSelected = !viewState.showPlayButton && !viewState.showLoading
                            textViewEpisodeTitle.text = viewState.title
                            textViewContributorTitle.text = viewState.subtitle

                            viewState.imageUrl?.let { imageUrl ->
                                imageLoader.load(
                                    imageViewPodcastEpisode,
                                    imageUrl,
                                    ImageLoaderOptions.Builder()
                                        .placeholderResId(chat.sphinx.podcast_player.R.drawable.ic_podcast_placeholder)
                                        .build()
                                )
                            }

                            imageViewForward30Button.goneIfFalse(!viewState.showLoading)
                            progressBarAudioLoading.goneIfFalse(viewState.showLoading)

                            scrollToBottom(callback = {
                                root.visible
                            })
                        }
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            tribeFeedViewModel.contributionsViewStateContainer.collect { viewState ->
                headerBinding.apply {

                    @Exhaustive
                    when (viewState) {
                        is PodcastContributionsViewState.Contributions -> {
                            textViewChatHeaderContributionsIcon.visible
                            textViewChatHeaderContributions.apply string@ {
                                this@string.visible
                                this@string.text = viewState.text
                            }
                        }
                        is PodcastContributionsViewState.None -> {
                            textViewChatHeaderContributionsIcon.gone
                            textViewChatHeaderContributions.gone
                        }
                    }

                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.tribePopupViewStateContainer.collect { viewState ->
                tribePopupBinding.apply {
                    @Exhaustive
                    when (viewState) {
                        is TribePopupViewState.Idle -> {
                            root.goneIfFalse(false)
                        }

                        is TribePopupViewState.TribeMemberPopup -> {
                            root.goneIfFalse(true)

                            layoutChatTribePopup.apply {
                                textViewInitials.apply {
                                    text = viewState.memberName.value.getInitials()
                                    setBackgroundRandomColor(
                                        chat.sphinx.chat_common.R.drawable.chat_initials_circle,
                                        Color.parseColor(
                                            userColorsHelper.getHexCodeForKey(
                                                viewState.colorKey,
                                                root.context.getRandomHexCode(),
                                            )
                                        ),
                                    )
                                }

                                viewState.memberPic?.let { photoUrl ->
                                    imageViewMemberProfilePicture.visible

                                    imageLoader.load(
                                        imageViewMemberProfilePicture,
                                        photoUrl.value,
                                        ImageLoaderOptions.Builder()
                                            .placeholderResId(chat.sphinx.podcast_player.R.drawable.ic_profile_avatar_circle)
                                            .transformation(Transformation.CircleCrop)
                                            .build()
                                    )
                                }

                                textViewMemberName.text = viewState.memberName.value
                            }
                        }
                    }
                }
            }
        }
    }
}
