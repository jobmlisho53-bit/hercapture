package com.camera.pro.camera

import android.content.Context
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.lifecycle.LifecycleOwner
import java.io.File

class CameraManager(private val context: Context) {
    
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var imageCapture: ImageCapture? = null
    
    fun bindCameraUseCases(
        previewView: PreviewView,
        onImageCaptured: (String) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder()
                .setTargetResolution(Size(1920, 1080))
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
            
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetResolution(Size(4032, 3024)) // Max quality
                .setDefaultFlashMode(ImageCapture.FLASH_MODE_OFF)
                .setJpegQuality(100)
                .build()
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    context as LifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture!!
                )
            } catch (exc: Exception) {
                Toast.makeText(context, "Camera binding failed: ${exc.message}", Toast.LENGTH_LONG).show()
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    fun takePicture(onComplete: (String) -> Unit) {
        val imageCapture = imageCapture ?: run {
            Toast.makeText(context, "Camera not ready", Toast.LENGTH_SHORT).show()
            return
        }
        
        val tempFile = File(context.cacheDir, "temp_capture_${System.currentTimeMillis()}.jpg")
        
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(tempFile)
            .build()
        
        imageCapture.takePicture(
            outputFileOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(context, "Capture failed: ${exception.message}", Toast.LENGTH_LONG).show()
                    exception.printStackTrace()
                }
                
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri?.toString() ?: tempFile.absolutePath
                    onComplete(if (savedUri.startsWith("file://")) savedUri.substring(7) else savedUri)
                }
            }
        )
    }
}
