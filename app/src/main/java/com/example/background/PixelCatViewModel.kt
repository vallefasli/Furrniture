package com.example.background

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
// ✨ FIX: Correct MediaType import
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.max

class PixelCatViewModel : ViewModel() {
    var isProcessing by mutableStateOf(false)
    var statusMessage by mutableStateOf("Ready")
    var detectedBreed by mutableStateOf<String?>(null)
    var generatedSticker by mutableStateOf<Bitmap?>(null)

    // --- API KEYS ---
    private val geminiApiKey = "AIzaSyCnxUy7QtAtxzb3x__PnU7ltXIHv9Cpz74"
    private val pixelLabSecret = "31d5a7b1-7ca9-4447-9307-dde7f7b35338"
    // ✨ YOUR PHOTOROOM KEY (from your screenshot)
    private val photoroomApiKey = "e8rFH24bQWKq5eY1qtVVsXLe"

    // ✨ MAX PATIENCE: 5 Minutes
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val geminiModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = geminiApiKey
    )

    fun identifyAndPixelate(context: Context, originalBitmap: Bitmap, onComplete: () -> Unit) {
        isProcessing = true
        statusMessage = "Starting..."

        viewModelScope.launch {
            try {
                // 1. PREPARE IMAGE
                val scaledBitmap = withContext(Dispatchers.Default) {
                    val scale = 128f / max(originalBitmap.width, originalBitmap.height)
                    if (scale < 1) {
                        Bitmap.createScaledBitmap(
                            originalBitmap,
                            (originalBitmap.width * scale).toInt(),
                            (originalBitmap.height * scale).toInt(),
                            true
                        )
                    } else originalBitmap
                }

                // 2. ASK GEMINI
                statusMessage = "Analyzing..."
                try {
                    val response = geminiModel.generateContent(content {
                        image(scaledBitmap)
                        text("Identify this cat's primary color and breed. Reply with ONLY 2-3 words (e.g. 'Black Bombay', 'Orange Tabby').")
                    })
                    detectedBreed = response.text?.trim() ?: "Cat"
                    statusMessage = "Found: $detectedBreed"
                } catch (e: Exception) {
                    detectedBreed = "Cat"
                }

                // 3. PIXELLAB
                statusMessage = "Drawing (Wait 30s+)..."
                val base64String = generatePixelArt(scaledBitmap, detectedBreed!!)

                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                val pixelBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                // 4. PHOTOROOM (Using your EXACT working logic)
                statusMessage = "Removing BG..."
                val transparentSticker = if (pixelBitmap != null) removeBackgroundWithPhotoroom(pixelBitmap) else null

                if (transparentSticker != null) {
                    generatedSticker = transparentSticker
                    statusMessage = "Done! ✨"
                } else {
                    generatedSticker = pixelBitmap
                    statusMessage = "Done (BG kept)"
                }

                onComplete()

            } catch (e: Exception) {
                statusMessage = "Failed: ${e.message}"
                Log.e("PixelCatVM", "Fatal", e)
                showToast(context, "Error: ${e.message}")
                onComplete()
            } finally {
                isProcessing = false
            }
        }
    }

    private suspend fun generatePixelArt(bitmap: Bitmap, simpleDescription: String): String {
        return withContext(Dispatchers.IO) {
            val base64Input = bitmapToBase64(bitmap)

            val jsonBody = JSONObject().apply {
                put("reference_image", base64Input)
                put("description", "A pixel art sticker of a $simpleDescription cat, vector, white background")
                put("image_size", JSONObject().apply { put("width", 128); put("height", 128) })
                put("guidance_scale", 7.5)
            }

            val request = Request.Builder()
                .url("https://api.pixellab.ai/v1/generate-image-bitforge")
                .addHeader("Authorization", "Bearer $pixelLabSecret")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                throw Exception("PixelLab ${response.code}: $responseBody")
            }

            val jsonResponse = JSONObject(responseBody ?: "")
            if (jsonResponse.has("image")) {
                jsonResponse.getJSONObject("image").getString("base64")
            } else {
                throw Exception("Empty Response")
            }
        }
    }

    // ✨ REPLACED WITH YOUR EXACT WORKING LOGIC
    private suspend fun removeBackgroundWithPhotoroom(bitmap: Bitmap): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val outputStream = ByteArrayOutputStream()
                // ✨ FIX 1: Use JPEG 90 (Matches your working code)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                val imageBytes = outputStream.toByteArray()

                // ✨ FIX 2: Use "cat_rescue.jpg" and "image/jpeg" (Matches your working code)
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "image_file",
                        "cat_rescue.jpg",
                        imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                    )
                    .build()

                val request = Request.Builder()
                    .url("https://sdk.photoroom.com/v1/segment")
                    .addHeader("x-api-key", photoroomApiKey)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBytes = response.body?.bytes()
                    if (responseBytes != null) {
                        BitmapFactory.decodeByteArray(responseBytes, 0, responseBytes.size)
                    } else null
                } else {
                    Log.e("PixelVM", "Photoroom Failed: ${response.code}")
                    null
                }
            } catch (e: Exception) {
                Log.e("PixelVM", "Exception", e)
                null
            }
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 40, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    private suspend fun showToast(context: Context, message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    // --- DATABASE FUNCTIONS ---
    fun saveCatToDatabase(context: Context, name: String, location: String, date: String, originalBitmap: Bitmap?) {
        viewModelScope.launch(Dispatchers.IO) {
            val imagePath = originalBitmap?.let { saveImageToInternalStorage(context, it, "photo_${System.currentTimeMillis()}") }
            val stickerPath = generatedSticker?.let { saveImageToInternalStorage(context, it, "sticker_${System.currentTimeMillis()}") }
            val newCat = CatItem(
                name = name, location = location, date = date,
                breed = detectedBreed ?: "Unknown",
                imagePath = imagePath, stickerPath = stickerPath,
                posX = 0f, posY = 0f
            )
            CatDatabase.getDatabase(context).catDao().insertCat(newCat)
            withContext(Dispatchers.Main) { resetState() }
        }
    }

    // Position Update Logic
    fun saveCatPosition(context: Context, cat: CatItem, newX: Float, newY: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedCat = cat.copy(posX = newX, posY = newY)
            CatDatabase.getDatabase(context).catDao().updateCat(updatedCat)
        }
    }

    private fun saveImageToInternalStorage(context: Context, bitmap: Bitmap, fileName: String): String {
        val file = File(context.filesDir, "$fileName.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        return file.absolutePath
    }

    fun resetState() {
        isProcessing = false
        statusMessage = "Ready"
        detectedBreed = null
        generatedSticker = null
    }
}