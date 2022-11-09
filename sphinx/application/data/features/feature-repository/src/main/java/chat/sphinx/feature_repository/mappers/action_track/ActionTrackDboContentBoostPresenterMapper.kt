package chat.sphinx.feature_repository.mappers.action_track

import chat.sphinx.conceptcoredb.ActionTrackDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.wrapper_action_track.ActionTrackId
import chat.sphinx.wrapper_action_track.ActionTrackMetaData
import chat.sphinx.wrapper_action_track.ActionTrackType
import chat.sphinx.wrapper_action_track.action_wrappers.*
import chat.sphinx.wrapper_action_track.toActionTrackUploaded
import com.squareup.moshi.Moshi
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

internal class ActionTrackDboContentBoostPresenterMapper(
    dispatchers: CoroutineDispatchers,
    val moshi: Moshi
): ClassMapper<ActionTrackDbo, ContentBoostAction?>(dispatchers) {

    override suspend fun mapFrom(value: ActionTrackDbo): ContentBoostAction? {
        return value.meta_data.value.toContentBoostActionOrNull(moshi)
    }

    override suspend fun mapTo(value: ContentBoostAction?): ActionTrackDbo {
        return ActionTrackDbo(
            id = ActionTrackId(Long.MAX_VALUE),
            type = ActionTrackType.Message,
            meta_data = ActionTrackMetaData(value?.toJson(moshi) ?: "{}"),
            uploaded = false.toActionTrackUploaded(),
        )
    }
}