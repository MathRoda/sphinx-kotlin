package chat.sphinx.example.delete_media_detail.ui

import android.app.Application
import android.content.*
import androidx.lifecycle.SavedStateHandle
import chat.sphinx.example.delete_media_detail.navigation.DeleteMediaDetailNavigator
import chat.sphinx.example.delete_media_detail.viewstate.DeleteMediaDetailViewState
import chat.sphinx.example.delete_media_detail.viewstate.DeleteNotificationViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import javax.inject.Inject

@HiltViewModel
internal class DeleteMediaDetailViewModel @Inject constructor(
    private val app: Application,
    val navigator: DeleteMediaDetailNavigator,
    dispatchers: CoroutineDispatchers,
    handle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        DeleteDetailNotifySideEffect,
        DeleteMediaDetailViewState
        >(dispatchers, DeleteMediaDetailViewState.Idle)
{
    val deleteNotificationViewStateContainer: ViewStateContainer<DeleteNotificationViewState> by lazy {
        ViewStateContainer(DeleteNotificationViewState.Closed)
    }

    fun deleteItem() {
        deleteNotificationViewStateContainer.updateViewState(DeleteNotificationViewState.Open)
    }

}
