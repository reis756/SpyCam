package com.reisdeveloper.hiddencam.lib

import android.annotation.SuppressLint
import android.content.Context
import android.util.Size
import androidx.camera.core.CameraX
import androidx.camera.core.FlashMode
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureConfig
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig
import androidx.camera.core.VideoCapture
import androidx.camera.core.VideoCaptureConfig
import java.io.File

@SuppressLint("RestrictedApi")
class HiddenVideo @JvmOverloads constructor(
    context: Context,
    private val baseFileDirectory: File,
    private val imageCapturedListener: OnImageCapturedListener,
    private val captureFrequency: CaptureTimeFrequency = CaptureTimeFrequency.OneShot,
    private val targetAspectRatio: TargetAspectRatio? = null,
    private val targetResolution: Size? = null,
    private val targetRotation: Int? = null,
    private val cameraType: CameraType = CameraType.FRONT_CAMERA,
    private val flashMode: FlashMode
)  {
    private val lifeCycleOwner = HiddenCamLifeCycleOwner()
    var isRecording = false

    /**
     * For some devices, if the camera doesn't have time to preview before the actual capture, it
     * would result into an underexposed or overexposed image. Hence, A [Preview] use case is set up
     * which renders to a dummy surface.
     */
    private lateinit var preview: Preview

    /** Configures the camera for preview */
    private var previewConfig = PreviewConfig.Builder().apply {
        setLensFacing(cameraType.lensFacing)
        if (targetRotation != null) setTargetRotation(targetRotation)
        if (targetAspectRatio != null) setTargetAspectRatio(targetAspectRatio.aspectRatio)
        if (targetResolution != null) setTargetResolution(targetResolution)
    }.build()

    /**
     * An [VideoCapture] use case to capture images.
     */
    private lateinit var videoCapture: VideoCapture

    /**
     * Configures the camera for Video Capture.
     */
    private var videoCaptureConfig: VideoCaptureConfig = VideoCaptureConfig.Builder().apply {
            setLensFacing(cameraType.lensFacing)
            if (targetRotation != null) setTargetRotation(targetRotation)
            if (targetAspectRatio != null) setTargetAspectRatio(targetAspectRatio.aspectRatio)
            setVideoFrameRate(24)
        }.build()

    init {
        if (context.hasPermissions()) {
            preview = Preview(previewConfig)
            videoCapture = VideoCapture(videoCaptureConfig)
            CameraX.bindToLifecycle(lifeCycleOwner, preview, videoCapture)
        } else throw SecurityException("You need to have access to both CAMERA and  WRITE_EXTERNAL_STOREGE permissions")
    }

    fun start() {
        lifeCycleOwner.start()
    }

    fun stop() {
        lifeCycleOwner.stop()
    }

    fun destroy() {
        lifeCycleOwner.tearDown()
    }

    fun captureVideo() {
        if (!isRecording) {
            preview.enableTorch(flashMode == FlashMode.ON)
            videoCapture.startRecording(
                createVideoFile(baseFileDirectory),
                MainThreadExecutor,
                object : VideoCapture.OnVideoSavedListener {
                    override fun onVideoSaved(file: File) {
                        imageCapturedListener.onImageCaptured(file)
                    }

                    override fun onError(
                        videoCaptureError: VideoCapture.VideoCaptureError,
                        message: String,
                        cause: Throwable?,
                    ) {
                        imageCapturedListener.onImageCaptureError(cause)
                    }
                }
            )
        } else {
            preview.enableTorch(false)
            videoCapture.stopRecording()
        }

        isRecording = !isRecording
    }
}