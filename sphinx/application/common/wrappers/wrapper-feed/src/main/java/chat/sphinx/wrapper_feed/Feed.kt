package chat.sphinx.wrapper_feed

import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedUrl

inline val Feed.isPodcast: Boolean
    get() = feedType.isPodcast()

inline val Feed.isVideo: Boolean
    get() = feedType.isVideo()

inline val Feed.isNewsletter: Boolean
    get() = feedType.isNewsletter()

data class Feed(
    val id: FeedId,
    val feedType: FeedType,
    val title: FeedTitle,
    val description: FeedDescription?,
    val feedUrl: FeedUrl,
    val author: FeedAuthor?,
    val generator: FeedGenerator?,
    val imageUrl: PhotoUrl?,
    val ownerUrl: FeedUrl?,
    val link: FeedUrl?,
    val datePublished: DateTime?,
    val dateUpdated: DateTime?,
    val contentType: FeedContentType?,
    val language: FeedLanguage?,
    val chatId: ChatId
) {

    var items: List<FeedItem> = listOf()

    var chat: Chat? = null

    var lastItem: FeedItem? = null
        get() {
            for (item in items) {
                if (item.id.value.toLongOrNull() == chat?.metaData?.itemId?.value) {
                    return item
                }
            }
            if (items.count() > 0) {
                return items.first()
            }
            return null
        }
}