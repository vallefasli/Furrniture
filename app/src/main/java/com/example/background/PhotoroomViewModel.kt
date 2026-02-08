package com.example.background

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class PhotoroomViewModel : ViewModel() {
    var imageSource by mutableStateOf<Uri?>(null)
    var isProcessing by mutableStateOf(false)
    var lastError by mutableStateOf<String?>(null)

    // ✨ Your updated Photoroom API Key
    private val apiKey = "e8rFH24bQWKq5eY1qtVVsXLe"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun updateImage(uri: Uri?) {
        imageSource = uri
        lastError = null
    }

    fun removeBackground(context: Context, imageUri: Uri, onComplete: (Uri) -> Unit) {
        isProcessing = true
        lastError = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Convert URI to ByteArray with compression
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream) ?: throw Exception("Failed to load bitmap")

                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                val imageBytes = outputStream.toByteArray()

                // 2. Prepare Photoroom SDK Request
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "image_file",
                        "cat_rescue.jpg",
                        imageBytes.toRequestBody("image/jpeg".toMediaType())
                    )
                    .build()

                val request = Request.Builder()
                    .url("https://sdk.photoroom.com/v1/segment")
                    .addHeader("x-api-key", apiKey)
                    .post(requestBody)
                    .build()

                // 3. Execute call and handle response
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Photoroom Error: ${response.code} - ${response.message}")
                    }

                    val responseBytes = response.body?.bytes() ?: throw Exception("Empty response body")

                    // 4. Save the resulting PNG sticker to internal cache
                    val stickerFile = File(context.cacheDir, "sticker_${System.currentTimeMillis()}.png")
                    FileOutputStream(stickerFile).use { it.write(responseBytes) }
                    val stickerUri = Uri.fromFile(stickerFile)

                    withContext(Dispatchers.Main) {
                        imageSource = stickerUri
                        onComplete(stickerUri)
                        isProcessing = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    lastError = e.message
                    isProcessing = false
                    // Fallback: Use original URI if processing fails
                    onComplete(imageUri)
                }
            }
        }
    }
}