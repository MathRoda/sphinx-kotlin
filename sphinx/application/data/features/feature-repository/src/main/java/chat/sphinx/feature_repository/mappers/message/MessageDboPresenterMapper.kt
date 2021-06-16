package chat.sphinx.feature_repository.mappers.message

import chat.sphinx.conceptcoredb.MessageDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.feature_repository.model.MessageDboWrapper
import chat.sphinx.wrapper_message.*
import com.squareup.moshi.Moshi
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.crypto_common.extensions.decodeToString
import kotlinx.coroutines.withContext
import okio.base64.decodeBase64ToArray

internal class MessageDboPresenterMapper(
    dispatchers: CoroutineDispatchers,
    private val moshi: Moshi,
): ClassMapper<MessageDbo, MessageDboWrapper>(dispatchers) {
    override suspend fun mapFrom(value: MessageDbo): MessageDboWrapper {
        return MessageDboWrapper(value).also { message ->
            value.message_content_decrypted?.let { decrypted ->
                if (message.type.isMessage()) {
                    when {
                        decrypted.value.startsWith("boost::") -> {
                            withContext(default) {
                                decrypted.value.split("::")
                                    .elementAtOrNull(1)
                                    ?.toPodBoostOrNull(moshi)
                                    ?.let { podBoost ->
                                        message._podBoost = podBoost
                                    }
                            }
                        }
                        decrypted.value.startsWith("giphy::") -> {
                            withContext(default) {
                                decrypted.value.split("::")
                                    .elementAtOrNull(1)
                                    ?.decodeBase64ToArray()
                                    ?.decodeToString()
                                    ?.toGiphyDataOrNull(moshi)
                                    ?.let { giphy ->
                                        message._giphyData = giphy
                                    }
                            }
                        }
                        // TODO: Handle podcast audio clips
                        // clip::{"ts":8818,"feedID":226249,"text":"Marty, I agree there is no climate emergency.","pubkey":"02683c5d0cf435fe8a0f42ba9a5999a98291476e82947707313cef69612000f718","itemID":2361506482,"title":"Rabbit Hole Recap: Bitcoin Week of 2021.05.10","url":"https://anchor.fm/s/558f520/podcast/play/33445131/https%3A%2F%2Fd3ctxlq1ktw2nl.cloudfront.net%2Fstaging%2F2021-4-13%2F186081984-44100-2-d892325769c3.m4a"}
                    }
                }

                message._messageContentDecrypted = decrypted
            }
        }
    }

    override suspend fun mapTo(value: MessageDboWrapper): MessageDbo {
        return value.messageDbo
    }
}