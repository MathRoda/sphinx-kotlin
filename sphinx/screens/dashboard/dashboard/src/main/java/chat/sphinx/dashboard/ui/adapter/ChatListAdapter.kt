package chat.sphinx.dashboard.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import app.cash.exhaustive.Exhaustive
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.ui.DashboardViewModel
import chat.sphinx.dashboard.ui.collectChatViewState
import chat.sphinx.dashboard.ui.currentChatViewState
import chat.sphinx.resources.setTextColorExt
import chat.sphinx.wrapper_chat.*
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.hhmmElseDate
import chat.sphinx.wrapper_message.isMessage
import io.matthewnelson.android_feature_screens.util.invisibleIfFalse
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

private typealias ChatViewHolder = ChatListAdapter.ChatViewHolder

@Suppress("NOTHING_TO_INLINE")
private inline fun ChatViewHolder.getChatHolderLayout(): ConstraintLayout =
    itemView.findViewById(R.id.layout_constraint_chat_holder)

@Suppress("NOTHING_TO_INLINE")
private inline fun ChatViewHolder.getImageViewChatHolder(): ImageView =
    itemView.findViewById(R.id.image_view_chat_holder)

@Suppress("NOTHING_TO_INLINE")
private inline fun ChatViewHolder.getTextViewName(): TextView =
    itemView.findViewById<TextView>(R.id.text_view_chat_holder_name)
        .also { it.setTextColorExt(android.R.color.white) }

@Suppress("NOTHING_TO_INLINE")
private inline fun ChatViewHolder.getImageViewLock(): ImageView =
    itemView.findViewById(R.id.image_view_chat_holder_lock)

@Suppress("NOTHING_TO_INLINE")
private inline fun ChatViewHolder.getTextViewMessage(): TextView =
    itemView.findViewById<TextView>(R.id.text_view_chat_holder_message)
        .also { it.setTextColorExt(R.color.textHint) }

@Suppress("NOTHING_TO_INLINE")
private inline fun ChatViewHolder.getImageViewNotification(): ImageView =
    itemView.findViewById(R.id.image_view_chat_holder_notification)

@Suppress("NOTHING_TO_INLINE")
private inline fun ChatViewHolder.getTextViewTime(): TextView =
    itemView.findViewById(R.id.text_view_chat_holder_time)

internal class ChatListAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: DashboardViewModel,
): RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>(), DefaultLifecycleObserver {

    inner class ChatViewHolder(view: View): RecyclerView.ViewHolder(view)

    private inner class Diff(
        private val oldList: List<DashboardChat>,
        private val newList: List<DashboardChat>,
    ): DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return try {
                oldList[oldItemPosition].let { old ->
                    newList[newItemPosition].let { new ->
                        // TODO: Clean up...
                        when {
                            old is DashboardChat.GroupOrTribe &&
                            new is DashboardChat.GroupOrTribe -> {
                                old.chat.id == new.chat.id &&
                                old.chat.latestMessageId == new.chat.latestMessageId
                            }
                            old is DashboardChat.Conversation &&
                            new is DashboardChat.Conversation -> {
                                old.contact.id == new.contact.id &&
                                old.chat?.id == new.chat?.id &&
                                old.chat?.latestMessageId == new.chat?.latestMessageId

                            }
                            else -> {
                                false
                            }
                        }
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                false
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return try {
                oldList[oldItemPosition].let { old ->
                    newList[newItemPosition].let { new ->
                        // TODO: Clean up...
                        when {
                            old is DashboardChat.GroupOrTribe &&
                            new is DashboardChat.GroupOrTribe -> {
                                old.chatName == new.chatName                            &&
                                old.chat.isMuted == new.chat.isMuted                    &&
                                old.chat.seen == new.chat.seen                          &&
                                old.chat.photoUrl == new.chat.photoUrl
                            }
                            old is DashboardChat.Conversation &&
                            new is DashboardChat.Conversation -> {
                                old.chatName == new.chatName                            &&
                                old.chat?.isMuted == new.chat?.isMuted                  &&
                                old.chat?.seen == new.chat?.seen                        &&
                                old.chat?.photoUrl == new.chat?.photoUrl
                            }
                            else -> {
                                false
                            }
                        }
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                false
            }
        }

    }

    private val dashboardChats = ArrayList<DashboardChat>(viewModel.currentChatViewState.list)
    private val supervisor = OnStartStopSupervisor(lifecycleOwner)

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        supervisor.scope().launch(viewModel.dispatchers.mainImmediate) {
            viewModel.collectChatViewState { viewState ->

                if (dashboardChats.isEmpty()) {
                    dashboardChats.addAll(viewState.list)
                    this@ChatListAdapter.notifyDataSetChanged()
                } else {
                    withContext(viewModel.dispatchers.default) {
                        DiffUtil.calculateDiff(
                            Diff(dashboardChats, viewState.list)
                        )
                    }.let { result ->
                        dashboardChats.clear()
                        dashboardChats.addAll(viewState.list)
                        result.dispatchUpdatesTo(this@ChatListAdapter)
                    }
                }

            }
        }
    }

    override fun getItemCount(): Int {
        return dashboardChats.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatListAdapter.ChatViewHolder {
        return ChatViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.layout_chat_holder, parent, false)
        )
    }

    private val today00: DateTime by lazy {
        DateTime.getToday00()
    }

    override fun onBindViewHolder(holder: ChatListAdapter.ChatViewHolder, position: Int) {
        dashboardChats.getOrNull(position)?.let { dashboardChat ->
            val layoutChatHolder = holder.getChatHolderLayout()
            val imageViewChatHolder = holder.getImageViewChatHolder()
            val textViewName = holder.getTextViewName()
            val imageViewLock = holder.getImageViewLock()
            val textViewMessage = holder.getTextViewMessage()
            val imageViewNotification = holder.getImageViewNotification()
            val textViewTime = holder.getTextViewTime()

            textViewName.text = if (dashboardChat.chatName != null) {
                dashboardChat.chatName
            } else {
                // Should never make it here, but just in case...
                textViewName.setTextColorExt(R.color.primaryRed)
                "ERROR: NULL NAME"
            }

            textViewTime.text = dashboardChat.message?.date?.hhmmElseDate(today00) ?: ""

            // TODO: Re-work once pulling of messages gets fleshed out
            val message = dashboardChat.message
            textViewMessage.text = when {
                message == null -> {
                    ""
                }
                message.decryptionError -> {
                    textViewMessage.setTextColorExt(R.color.primaryRed)
                    "DECRYPTION ERROR..."
                }
                message.type.isMessage() -> {
                    // TODO: if from owner use "you: "
                    val sender: String = message.senderAlias?.value?.let { alias ->
                        "$alias: "
                    } ?: ""

                    message.messageContentDecrypted?.value?.let { decrypted ->
                        "$sender$decrypted"
                    } ?: "$sender..."
                }
                else -> {
                    ""
                }
            }

            imageViewNotification.invisibleIfFalse(dashboardChat.chat?.isMuted() != false)

            layoutChatHolder.setOnClickListener {
                @Exhaustive
                when (dashboardChat) {
                    is DashboardChat.Conversation -> {
                        // TODO: Use ContactId
                        dashboardChat.chat?.id?.let {
                            supervisor.scope().launch {
                                viewModel.dashboardNavigator.toChatContact(it)
                            }
                        }
                    }
                    is DashboardChat.GroupOrTribe -> {
                        if (dashboardChat.chat.type.isGroup()) {
                            supervisor.scope().launch {
                                viewModel.dashboardNavigator.toChatGroup(dashboardChat.chat.id)
                            }
                        } else if (dashboardChat.chat.type.isTribe()) {
                            supervisor.scope().launch {
                                viewModel.dashboardNavigator.toChatTribe(dashboardChat.chat.id)
                            }
                        }
                    }
                }
            }
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }
}