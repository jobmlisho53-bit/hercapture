package com.camera.pro.storage

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream

class ImageStorage {
    
    fun saveImageToGallery(context: Context, imagePath: String) {
        val sourceFile = File(imagePath)
        if (!sourceFile.exists()) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ - Use MediaStore API
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "CameraPro_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CameraPro")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )
            
            uri?.let { contentUri ->
                context.contentResolver.openOutputStream(contentUri)?.use { outputStream ->
                    FileInputStream(sourceFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                // Mark as not pending
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(contentUri, values, null, null)
            }
        } else {
            // Android 9 and below - Direct file copy
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val cameraProDir = File(picturesDir, "CameraPro")
            cameraProDir.mkdirs()
            
            val destFile = File(cameraProDir, "CameraPro_${System.currentTimeMillis()}.jpg")
            sourceFile.copyTo(destFile, overwrite = true)
        }
    }
}
