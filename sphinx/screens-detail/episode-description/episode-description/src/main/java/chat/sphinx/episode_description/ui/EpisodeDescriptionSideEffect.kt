package chat.sphinx.episode_description.ui

import android.content.Context
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal class EpisodeDescriptionSideEffect(val value: String): SideEffect<Context>() {
    override suspend fun execute(value: Context) {
        SphinxToastUtils().show(value, this.value)
    }

}
