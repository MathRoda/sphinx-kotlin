package chat.sphinx.camera.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ExifInterface
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.camera.R
import chat.sphinx.camera.databinding.FragmentCameraBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import com.example.android.camera.utils.OrientationLiveData
import com.example.android.camera.utils.computeExifOrientation
import com.example.android.camera.utils.getPreviewOutputSize
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@AndroidEntryPoint
internal class CameraFragment: SideEffectFragment<
        FragmentActivity,
        CameraSideEffect,
        CameraViewState,
        CameraViewModel,
        FragmentCameraBinding,
        >(R.layout.fragment_camera)
{
    companion object {
        const val ANIMATION_FAST_MILLIS = 50L
        const val ANIMATION_SLOW_MILLIS = 100L

        const val IMAGE_BUFFER_SIZE = 3
        const val IMAGE_CAPTURE_TIMEOUT_MILLIS: Long = 5_000
    }

    @Suppress("PrivatePropertyName")
    private val PERMISSIONS_REQUIRED = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
    )

    override val binding: FragmentCameraBinding by viewBinding(FragmentCameraBinding::bind)
    override val viewModel: CameraViewModel by viewModels()

    private val requestPermissionLauncher by lazy(LazyThreadSafetyMode.NONE) {
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { response ->

            try {
                for (permission in PERMISSIONS_REQUIRED) {
                    if (response[permission] != true) {
                        throw Exception()
                    }
                }

                startCamera()
            } catch (e: Exception) {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.submitSideEffect(
                        CameraSideEffect.Notify(getString(R.string.camera_permissions_required))
                    )
                }
            }
        }
    }

    private inner class ThreadHolder: DefaultLifecycleObserver {

        @Volatile
        private var thread: HandlerThread? = null
        private val threadLock = Object()

        @Volatile
        private var handler: Handler? = null
        private val handlerLock = Object()

        fun getThread(): HandlerThread =
            thread ?: synchronized(threadLock) {
                thread ?: HandlerThread("CameraThread").apply { start() }
                    .also {
                        thread = it
                        lifecycleScope.launch(viewModel.main) {
                            viewLifecycleOwner.lifecycle.addObserver(this@ThreadHolder)
                        }
                    }
            }

        fun getHandler(): Handler =
            handler ?: synchronized(handlerLock) {
                handler ?: Handler(getThread().looper)
                    .also { handler = it }
            }

        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            synchronized(handlerLock) {
                synchronized(threadLock) {
                    val thread = thread
                    handler = null
                    this.thread = null
                    thread?.quitSafely()
                }
            }
        }
    }

    private val cameraThreadHolder = ThreadHolder()
    private val imageReaderThreadHolder = ThreadHolder()

    private val frontCamera: CameraViewModel.CameraListItem? by lazy {
        viewModel.getFrontCamera()
    }

    private val backCamera: CameraViewModel.CameraListItem? by lazy {
        viewModel.getBackCamera()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel
    }

    private var orientationLiveData: OrientationLiveData? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as InsetterActivity)
            .addNavigationBarPadding(binding.includeCameraFooter.root)

        backCamera?.let { cameraItem ->
            binding.autoFitSurfaceViewCamera.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceDestroyed(holder: SurfaceHolder) {}
                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {}

                override fun surfaceCreated(holder: SurfaceHolder) {
                    val previewSize = getPreviewOutputSize(
                        binding.autoFitSurfaceViewCamera.display,
                        cameraItem.characteristics,
                        SurfaceHolder::class.java,
                    )

                    binding.autoFitSurfaceViewCamera.setAspectRatio(previewSize.width, previewSize.height)

                    view.post() {
                        if (hasPermissions(requireContext())) {
                            startCamera()
                        }
                    }
                }
            })

            orientationLiveData = OrientationLiveData(binding.root.context, cameraItem.characteristics).apply {
                observe(viewLifecycleOwner, Observer { orientation ->

                })
            }
        }

        if (!hasPermissions(requireContext())) {
            requestPermissionLauncher.launch(PERMISSIONS_REQUIRED)
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            camera?.close()
        } catch (e: Throwable) {}
    }

    private fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    private var camera: CameraDevice? = null
    private var imageReader: ImageReader? = null
    private var session: CameraCaptureSession? = null

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun startCamera() {
        backCamera?.let { cameraItem ->
            lifecycleScope.launch(viewModel.main) {
                val camera = openCamera(
                    viewModel.cameraManager,
                    cameraItem.cameraId,
                    cameraThreadHolder.getHandler(),
                ).also { camera = it }

                val size = cameraItem.configMap.getOutputSizes(ImageFormat.JPEG)
                    .maxByOrNull { it.height * it.width }!!

                val imageReader = ImageReader.newInstance(
                    size.width, size.height, ImageFormat.JPEG, IMAGE_BUFFER_SIZE
                ).also { imageReader = it }

                val targets = listOf(binding.autoFitSurfaceViewCamera.holder.surface, imageReader.surface)

                val session = createCaptureSession(
                    camera,
                    targets,
                    cameraThreadHolder.getHandler()
                ).also { session = it }

                val captureRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                    addTarget(binding.autoFitSurfaceViewCamera.holder.surface)
                }

                session.setRepeatingRequest(captureRequest.build(), null, cameraThreadHolder.getHandler())

                binding.includeCameraFooter.imageViewCameraFooterShutter.setOnClickListener {

                    it.isEnabled = false

                    lifecycleScope.launch(viewModel.io) {
                        takePhoto(
                            cameraItem,
                            imageReader,
                            session,
                        ).use { result ->

                            val output = saveResult(cameraItem, result)

                            // If the result is a JPEG file, update EXIF metadata with orientation info
                            if (output.extension == "jpg") {
                                val exif = ExifInterface(output.absolutePath)
                                exif.setAttribute(
                                    ExifInterface.TAG_ORIENTATION,
                                    result.orientation.toString()
                                )
                                exif.saveAttributes()
                            }

                            // TODO: Display photo to user
//                            lifecycleScope.launch(viewModel.main) {
//
//                            }
                        }

                        it.post { it.isEnabled = true }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun openCamera(
        manager: CameraManager,
        cameraId: String,
        handler: Handler? = null
    ): CameraDevice = suspendCancellableCoroutine { cont ->
        manager.openCamera(
            cameraId,
            object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) = cont.resume(device)

                override fun onDisconnected(device: CameraDevice) {}

                override fun onError(device: CameraDevice, error: Int) {
                    val msg = when(error) {
                        ERROR_CAMERA_DEVICE -> "Fatal (device)"
                        ERROR_CAMERA_DISABLED -> "Device policy"
                        ERROR_CAMERA_IN_USE -> "Camera in use"
                        ERROR_CAMERA_SERVICE -> "Fatal (service)"
                        ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                        else -> "Unknown"
                    }
                    val exc = RuntimeException("Camera $cameraId error: ($error) $msg")
                    if (cont.isActive) cont.resumeWithException(exc)
                }
            },
            handler,
        )
    }

    /**
     * Starts a [CameraCaptureSession] and returns the configured session (as the result of the
     * suspend coroutine
     */
    private suspend fun createCaptureSession(
        device: CameraDevice,
        targets: List<Surface>,
        handler: Handler? = null
    ): CameraCaptureSession = suspendCoroutine { cont ->

        // Create a capture session using the predefined targets; this also involves defining the
        // session state callback to be notified of when the session is ready
        device.createCaptureSession(targets, object: CameraCaptureSession.StateCallback() {

            override fun onConfigured(session: CameraCaptureSession) = cont.resume(session)

            override fun onConfigureFailed(session: CameraCaptureSession) {
                val exc = RuntimeException("Camera ${device.id} session configuration failed")
                cont.resumeWithException(exc)
            }
        }, handler)
    }

    private val animationTask: Runnable by lazy {
        Runnable {
            binding.viewCameraOverlay.apply{
                background = Color.argb(150, 255, 255, 255).toDrawable()
                postDelayed(
                    {
                        background = null
                    },
                    ANIMATION_FAST_MILLIS
                )
            }
        }
    }

    private suspend fun takePhoto(
        cameraListItem: CameraViewModel.CameraListItem,
        imageReader: ImageReader,
        session: CameraCaptureSession
    ): CombinedCaptureResult =
        suspendCoroutine { cont ->

            // Flush any images left in the image reader
            @Suppress("ControlFlowWithEmptyBody")
            while (imageReader.acquireNextImage() != null) {}

            val imageQueue = ArrayBlockingQueue<Image>(IMAGE_BUFFER_SIZE)
            imageReader.setOnImageAvailableListener(
                { reader ->
                    val image = reader.acquireNextImage()
                    imageQueue.add(image)
                },
                cameraThreadHolder.getHandler()
            )

            val captureRequest = session
                .device
                .createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                    addTarget(imageReader.surface)
                }

            session.capture(
                captureRequest.build(),
                object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureStarted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        timestamp: Long,
                        frameNumber: Long
                    ) {
                        super.onCaptureStarted(session, request, timestamp, frameNumber)
                        binding.autoFitSurfaceViewCamera.post(animationTask)
                    }

                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        super.onCaptureCompleted(session, request, result)
                        val resultTimestamp = result.get(CaptureResult.SENSOR_TIMESTAMP)

                        // Set a timeout in case image captured is dropped from the pipeline
                        val exc = TimeoutException("Image dequeuing took too long")
                        val timeoutRunnable = Runnable { cont.resumeWithException(exc) }

                        imageReaderThreadHolder.getHandler().postDelayed(timeoutRunnable, IMAGE_CAPTURE_TIMEOUT_MILLIS)

                        // Loop in the coroutine's context until an image with matching timestamp comes
                        // We need to launch the coroutine context again because the callback is done in
                        //  the handler provided to the `capture` method, not in our coroutine context
                        @Suppress("BlockingMethodInNonBlockingContext")
                        lifecycleScope.launch(cont.context) {
                            while (true) {
                                // Dequeue images while timestamps don't match
                                val image = imageQueue.take()

                                if (
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                                    image.format != ImageFormat.DEPTH_JPEG &&
                                    image.timestamp != resultTimestamp
                                ) {
                                    continue
                                }

                                // Unset the image reader listener
                                imageReaderThreadHolder.getHandler().removeCallbacks(timeoutRunnable)
                                imageReader.setOnImageAvailableListener(null, null)

                                // Clear the queue of images, if there are left
                                while (imageQueue.size > 0) {
                                    imageQueue.take().close()
                                }

                                // Compute EXIF orientation metadata
                                val rotation = orientationLiveData?.value ?: Surface.ROTATION_0
                                val mirrored = cameraListItem
                                    .characteristics
                                    .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
                                val exifOrientation = computeExifOrientation(rotation, mirrored)

                                // Build the result and resume progress
                                cont.resume(
                                    CombinedCaptureResult(
                                        image,
                                        result,
                                        exifOrientation,
                                        imageReader.imageFormat
                                    )
                                )
                            }
                        }
                    }
                },
                cameraThreadHolder.getHandler()
            )
    }

    private suspend fun saveResult(
        cameraItem: CameraViewModel.CameraListItem,
        result: CombinedCaptureResult,
    ): File = suspendCoroutine { cont ->
        when (result.format) {

            ImageFormat.JPEG,
            ImageFormat.DEPTH_JPEG -> {
                val buffer = result.image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining()).apply { buffer.get(this) }

                try {
                    val output = createFile("jpg")
                    FileOutputStream(output).use { it.write(bytes) }
                    cont.resume(output)
                } catch (e: IOException) {
                    cont.resumeWithException(e)
                }
            }

            ImageFormat.RAW_SENSOR -> {
                val dngCreator = DngCreator(cameraItem.characteristics, result.metadata)

                try {
                    val output = createFile("dng")
                    FileOutputStream(output).use { dngCreator.writeImage(it, result.image) }
                } catch (e: IOException) {
                    cont.resumeWithException(e)
                }
            }

            else -> {
                cont.resumeWithException(RuntimeException("Unknown image format: ${result.image.format}"))
            }
        }
    }

    private fun createFile(extension: String): File {
        val sdf = SimpleDateFormat("yyy_MM_dd_HH_mm_ss_SSS", Locale.getDefault())
        return File(binding.root.context.filesDir, "IMG_${sdf.format(Date())}.$extension")
    }

    data class CombinedCaptureResult(
        val image: Image,
        val metadata: CaptureResult,
        val orientation: Int,
        val format: Int
    ) : Closeable {
        override fun close() = image.close()
    }

    override suspend fun onSideEffectCollect(sideEffect: CameraSideEffect) {
        sideEffect.execute(requireActivity())
    }

    override suspend fun onViewStateFlowCollect(viewState: CameraViewState) {
//        TODO("Not yet implemented")
    }
}
