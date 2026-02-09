package com.camera.pro.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.ToggleButton
import android.view.View
import android.graphics.BitmapFactory
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {
    
    private lateinit var previewView: PreviewView
    private lateinit var captureButton: Button
    private lateinit var enhanceToggle: ToggleButton
    private lateinit var imageView: ImageView
    private lateinit var cameraManager: CameraManager
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var aiEnhancer: AiEnhancer
    private lateinit var imageStorage: ImageStorage
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it == true }
        if (granted) {
            setupCamera()
        } else {
            Toast.makeText(this, "Permissions required for camera access", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeComponents()        requestPermissions()
        
        // Setup reset button
        findViewById<Button>(R.id.reset_button).setOnClickListener {
            resetCamera()
        }
    }
    
    private fun initializeComponents() {
        previewView = findViewById(R.id.preview_view)
        captureButton = findViewById(R.id.capture_button)
        enhanceToggle = findViewById(R.id.enhance_toggle)
        imageView = findViewById(R.id.image_view)
        
        cameraManager = CameraManager(this)
        imageProcessor = ImageProcessor()
        aiEnhancer = AiEnhancer()
        imageStorage = ImageStorage()
        
        captureButton.setOnClickListener {
            takeAndProcessPhoto()
        }
    }
    
    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        
        val neededPermissions = permissions.filter { 
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED 
        }.toTypedArray()
        
        if (neededPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(neededPermissions)
        } else {
            setupCamera()
        }
    }
    
    private fun setupCamera() {
        cameraManager.bindCameraUseCases(previewView) { imagePath ->
            showProcessingPreview(imagePath)
        }
    }
    
    private fun takeAndProcessPhoto() {
        cameraManager.takePicture { originalImagePath ->            CoroutineScope(Dispatchers.Main).launch {
                // Show loading state
                captureButton.isEnabled = false
                captureButton.text = "PROCESSING..."
                
                try {
                    // Process image with OpenCV
                    val processedPath = imageProcessor.processImage(originalImagePath, this@MainActivity)
                    
                    // Apply AI enhancement if enabled
                    val finalPath = if (enhanceToggle.isChecked) {
                        aiEnhancer.upscaleImage(processedPath, this@MainActivity)
                    } else {
                        processedPath
                    }
                    
                    // Save to gallery
                    imageStorage.saveImageToGallery(this@MainActivity, finalPath)
                    
                    // Update UI
                    showFinalImage(finalPath)
                    Toast.makeText(this@MainActivity, "Image saved!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                } finally {
                    captureButton.isEnabled = true
                    captureButton.text = "CAPTURE"
                }
            }
        }
    }
    
    private fun showProcessingPreview(imagePath: String) {
        // Switch to preview mode
        previewView.visibility = View.GONE
        imageView.visibility = View.VISIBLE
        val bitmap = BitmapFactory.decodeFile(imagePath)
        imageView.setImageBitmap(bitmap)
    }
    
    private fun showFinalImage(imagePath: String) {
        val bitmap = BitmapFactory.decodeFile(imagePath)
        imageView.setImageBitmap(bitmap)
    }
    
    fun resetCamera() {
        // Hide image view, show camera preview
        imageView.visibility = View.GONE
        previewView.visibility = View.VISIBLE        // Rebind camera
        setupCamera()
    }
}
