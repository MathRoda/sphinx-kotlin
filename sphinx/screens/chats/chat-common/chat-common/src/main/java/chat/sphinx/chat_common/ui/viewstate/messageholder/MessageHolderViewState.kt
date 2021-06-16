package chat.sphinx.chat_common.ui.viewstate.messageholder

import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.isConversation
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_message.*

internal inline val MessageHolderViewState.isReceived: Boolean
    get() = this is MessageHolderViewState.Received

internal inline val MessageHolderViewState.showReceivedBubbleArrow: Boolean
    get() = background is BubbleBackground.First && this is MessageHolderViewState.Received

internal val MessageHolderViewState.showSentBubbleArrow: Boolean
    get() = background is BubbleBackground.First && this is MessageHolderViewState.Sent

internal sealed class MessageHolderViewState(
    val message: Message,
    chat: Chat,
    val background: BubbleBackground,
    val initialHolder: InitialHolderViewState,
    val messageSenderName: (Message) -> String,
    val accountOwner: () -> Contact,
) {

    companion object {
        val unsupportedMessageTypes: List<MessageType> by lazy {
            listOf(
                MessageType.Attachment,
                MessageType.BotRes,
                MessageType.Invoice,
                MessageType.Payment,
                MessageType.GroupAction.TribeDelete,
            )
        }
    }

    val unsupportedMessageType: LayoutState.Bubble.ContainerMiddle.UnsupportedMessageType? by lazy(LazyThreadSafetyMode.NONE) {
        if (unsupportedMessageTypes.contains(message.type)) {
            LayoutState.Bubble.ContainerMiddle.UnsupportedMessageType(
                messageType = message.type,
                gravityStart = this is Received,
            )
        } else {
            null
        }
    }

    val statusHeader: LayoutState.MessageStatusHeader? by lazy(LazyThreadSafetyMode.NONE) {
        if (background is BubbleBackground.First) {
            LayoutState.MessageStatusHeader(
                if (chat.type.isConversation()) null else message.senderAlias?.value,
                this is Sent,
                this is Sent && (message.status.isReceived() || message.status.isConfirmed()),
                message.messageContentDecrypted != null,
                DateTime.getFormathmma().format(message.date.value),
            )
        } else {
            null
        }
    }

    val deletedMessage: LayoutState.DeletedMessage? by lazy(LazyThreadSafetyMode.NONE) {
        if (message.status.isDeleted()) {
            LayoutState.DeletedMessage(
                gravityStart = this is Received,
                timestamp = DateTime.getFormathmma().format(message.date.value)
            )
        } else {
            null
        }
    }

    val bubbleDirectPayment: LayoutState.Bubble.ContainerTop.DirectPayment? by lazy(LazyThreadSafetyMode.NONE) {
        if (message.type.isDirectPayment()) {
            LayoutState.Bubble.ContainerTop.DirectPayment(showSent = this is Sent, amount = message.amount)
        } else {
            null
        }
    }

    val bubbleMessage: LayoutState.Bubble.ContainerMiddle.Message? by lazy(LazyThreadSafetyMode.NONE) {
        message.retrieveTextToShow()?.let { text ->
            if (text.isNotEmpty()) {
                LayoutState.Bubble.ContainerMiddle.Message(text = text)
            } else {
                null
            }
        }
    }

    val bubblePaidMessageDetails: LayoutState.Bubble.ContainerBottom.PaidMessageDetails? by lazy(LazyThreadSafetyMode.NONE) {
        if (!message.isPaidMessage) {
            null
        } else {
            val isPaymentPending = message.status.isPending()

            message.type.let { type ->
                LayoutState.Bubble.ContainerBottom.PaidMessageDetails(
                    amount = message.amount,
                    purchaseType = if (type.isPurchase()) type else null,
                    isShowingReceivedMessage = this is Received,
                    showPaymentAcceptedIcon = type.isPurchaseAccepted(),
                    showPaymentProgressWheel = type.isPurchaseProcessing(),
                    showSendPaymentIcon = this !is Sent && !isPaymentPending,
                    showPaymentReceivedIcon = this is Sent && !isPaymentPending,
                )
            }
        }
    }

    val bubblePaidMessageSentStatus: LayoutState.Bubble.ContainerTop.PaidMessageSentStatus? by lazy(LazyThreadSafetyMode.NONE) {
        if (!message.isPaidMessage || this !is Sent) {
            null
        } else {
            message.type.let { type ->
                LayoutState.Bubble.ContainerTop.PaidMessageSentStatus(
                    amount = message.amount,
                    purchaseType = if (type.isPurchase()) type else null,
                )
            }
        }
    }

    val bubbleGiphy: LayoutState.Bubble.ContainerTop.Giphy? by lazy(LazyThreadSafetyMode.NONE) {
        message.giphyData?.let {
            if (it.url.isNotEmpty()) {
                LayoutState.Bubble.ContainerTop.Giphy(it.url.replace("giphy.gif", "200w.gif"))
            } else {
                null
            }
        }
    }

    // don't use by lazy as this uses a for loop and needs to be initialized on a background
    // thread (so, while the MHVS is being created)
    val bubbleReactionBoosts: LayoutState.Bubble.ContainerBottom.Boost? =
        message.reactions?.let { nnReactions ->
            if (nnReactions.isEmpty()) {
                null
            } else {
                val set: MutableSet<BoostReactionImageHolder> = LinkedHashSet(1)
                var total: Long = 0
                for (reaction in nnReactions) {
//                    if (chatType?.isConversation() != true) {
//                        reaction.senderPic?.value?.let { url ->
//                            set.add(SenderPhotoUrl(url))
//                        } ?: reaction.senderAlias?.value?.let { alias ->
//                            set.add(SenderInitials(alias.getInitials()))
//                        }
//                    }
                    total += reaction.amount.value
                }

//                if (chatType?.isConversation() == true) {
//
//                    // TODO: Use Account Owner Initial Holder depending on sent/received
//                    @Exhaustive
//                    when (initialHolder) {
//                        is InitialHolderViewState.Initials -> {
//                            set.add(SenderInitials(initialHolder.initials))
//                        }
//                        is InitialHolderViewState.None -> {}
//                        is InitialHolderViewState.Url -> {
//                            set.add(SenderPhotoUrl(initialHolder.photoUrl.value))
//                        }
//                    }
//                }

                LayoutState.Bubble.ContainerBottom.Boost(
                    totalAmount = Sat(total),
                    senderPics = set,
                )
            }
        }

    val bubbleReplyMessage: LayoutState.Bubble.ContainerTop.ReplyMessage? by lazy {
        message.replyMessage?.let { nnMessage ->
            LayoutState.Bubble.ContainerTop.ReplyMessage(
                messageSenderName(nnMessage),

                nnMessage.retrieveTextToShow() ?: "",
            )
        }
    }

    val groupActionIndicator: LayoutState.GroupActionIndicator? by lazy(LazyThreadSafetyMode.NONE) {
        if (
            !message.type.isGroupAction() ||
            message.senderAlias == null
        ) {
            null
        } else {
            LayoutState.GroupActionIndicator(
                actionType = message.type as MessageType.GroupAction,
                isAdminView = if (chat.ownerPubKey == null || accountOwner().nodePubKey == null) {
                    false
                } else {
                    chat.ownerPubKey == accountOwner().nodePubKey
                },
                chatType = chat.type,
                subjectName = message.senderAlias!!.value
            )
        }
    }


    class Sent(
        message: Message,
        chat: Chat,
        background: BubbleBackground,
        replyMessageSenderName: (Message) -> String,
        accountOwner: () -> Contact,
    ) : MessageHolderViewState(
        message,
        chat,
        background,
        InitialHolderViewState.None,
        replyMessageSenderName,
        accountOwner,
    )

    class Received(
        message: Message,
        chat: Chat,
        background: BubbleBackground,
        initialHolder: InitialHolderViewState,
        replyMessageSenderName: (Message) -> String,
        accountOwner: () -> Contact,
    ) : MessageHolderViewState(
        message,
        chat,
        background,
        initialHolder,
        replyMessageSenderName,
        accountOwner,
    )
}