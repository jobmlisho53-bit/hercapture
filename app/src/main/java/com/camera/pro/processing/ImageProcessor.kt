package com.camera.pro.processing

import android.content.Context
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.photo.Photo
import org.opencv.imgcodecs.Imgcodecs
import java.io.File

class ImageProcessor {
    
    init {
        if (!OpenCVLoader.initDebug()) {
            throw RuntimeException("OpenCV initialization failed")
        }
    }
    
    fun processImage(inputPath: String, context: Context): String {
        val src = Imgcodecs.imread(inputPath)
        if (src.empty()) {
            throw RuntimeException("Could not load image: $inputPath")
        }
        
        val processed = Mat()
        
        // Step 1: Noise reduction
        Photo.fastNlMeansDenoisingColored(src, processed, 10.0, 10.0, 7, 21)
        
        // Step 2: Edge-aware sharpening
        val blurred = Mat()
        Imgproc.GaussianBlur(processed, blurred, Size(0.0, 0.0), 3.0)
        val sharpened = Mat()
        Core.addWeighted(processed, 1.8, blurred, -0.8, 0.0, sharpened)
        
        // Step 3: Contrast enhancement
        val enhanced = Mat()
        Imgproc.cvtColor(sharpened, enhanced, Imgproc.COLOR_BGR2LAB)
        
        val channels = ArrayList<Mat>()
        Core.split(enhanced, channels)
        
        val clahe = Imgproc.createCLAHE(2.0, Size(8, 8))
        clahe.apply(channels[0], channels[0])
        
        Core.merge(channels, enhanced)
        Imgproc.cvtColor(enhanced, sharpened, Imgproc.COLOR_LAB2BGR)
        
        val outputPath = File(context.cacheDir, "processed_${System.currentTimeMillis()}.jpg").absolutePath
        Imgcodecs.imwrite(outputPath, sharpened)
        
        // Release resources
        src.release()
        processed.release()
        blurred.release()
        sharpened.release()
        enhanced.release()
        
        return outputPath
    }
}
