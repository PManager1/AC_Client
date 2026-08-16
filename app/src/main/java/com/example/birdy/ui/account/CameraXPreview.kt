package com.example.birdy.ui.account

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.collectLatest

private const val TAG = "CameraXPreview"

/**
 * A live CameraX preview that captures the current camera frame as a rotated [Bitmap]
 * when [trigger] emits, delivering it via [onImageCaptured].
 *
 * The camera provider is bound asynchronously (never blocking the main thread) and only
 * after the [PreviewView] is realized, avoiding the classic `ListenableFuture.get()` on the
 * main thread deadlock. Camera errors surface via [onError] so the UI can show a message
 * instead of a silent black box.
 */
@Composable
fun CameraXPreview(
    trigger: kotlinx.coroutines.flow.Flow<Unit>,
    onImageCaptured: (Bitmap) -> Unit,
    onError: ((String) -> Unit)? = null,
    retryKey: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val hasPermission = remember {
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()
    }

    var cameraReady by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var previewReady by remember { mutableStateOf(false) }

    // The PreviewView instance owned by composition so the camera can bind to it.
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    // Bind the camera asynchronously once the preview view is realized and permission is granted.
    LaunchedEffect(lifecycleOwner, hasPermission, previewReady, retryKey) {
        if (!hasPermission || !previewReady) return@LaunchedEffect
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
            ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            try {
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                provider.unbindAll()

                val selector = when {
                    hasCamera(provider, CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                    hasCamera(provider, CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> null
                }

                if (selector == null) {
                    cameraReady = false
                    cameraError = "No camera found on this device. Enable the camera in the emulator/device settings."
                    onError?.invoke(cameraError.orEmpty())
                } else {
                    provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                    cameraReady = true
                    cameraError = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
                cameraReady = false
                cameraError = e.message ?: "Camera failed to start"
                onError?.invoke(cameraError.orEmpty())
            }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(Unit) {
        onDispose {
            val provider = try { ProcessCameraProvider.getInstance(context).get() } catch (e: Exception) { null }
            provider?.unbindAll()
        }
    }

    LaunchedEffect(trigger) {
        trigger.collectLatest {
            previewView.post {
                if (cameraReady) {
                    captureImage(context, imageCapture) { bitmap ->
                        onImageCaptured(bitmap)
                    }
                }
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            previewReady = true
            previewView
        }
    )
}

private fun hasCamera(provider: ProcessCameraProvider, selector: CameraSelector): Boolean =
    try {
        provider.hasCamera(selector)
    } catch (e: Exception) {
        false
    }

@SuppressLint("UnsafeOptInUsageError")
private fun captureImage(
    context: Context,
    imageCapture: ImageCapture,
    onCaptured: (Bitmap) -> Unit
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = image.toBitmap().rotated(image.imageInfo.rotationDegrees)
                image.close()
                onCaptured(bitmap)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Capture failed", exception)
            }
        }
    )
}

/** Decodes the image planes into a [Bitmap]. */
@OptIn(ExperimentalGetImage::class)
private fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    buffer.rewind()
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

/** Rotates a bitmap upright given a rotation value in degrees. */
private fun Bitmap.rotated(rotationDegrees: Int): Bitmap {
    if (rotationDegrees % 360 == 0) return this
    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    val rotated = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    if (rotated != this) recycle()
    return rotated
}
