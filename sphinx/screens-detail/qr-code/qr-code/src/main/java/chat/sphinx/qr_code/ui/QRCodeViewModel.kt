package chat.sphinx.qr_code.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.qr_code.navigation.QRCodeNavigator
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class QRCodeViewModel @Inject constructor(
    private val app: Application,
    val navigator: QRCodeNavigator,
    dispatchers: CoroutineDispatchers,
    handle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        NotifySideEffect,
        QRCodeViewState
        >(
            dispatchers,
            handle.navArgs<QRCodeFragmentArgs>().let {
                QRCodeViewState(
                    it.value.argShowBackArrow,
                    it.value.qrText,
                    null
                )
            },
        )
{
    val args: QRCodeFragmentArgs by handle.navArgs()

    init {
        viewModelScope.launch(default) {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(args.qrText, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }

            updateViewState(
                QRCodeViewState(
                    currentViewState.showBackButton,
                    currentViewState.qrText,
                    bitmap
                )
            )
        }
    }
}
