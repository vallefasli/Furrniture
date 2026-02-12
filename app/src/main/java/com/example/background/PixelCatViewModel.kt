package com.example.background

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

enum class RescueMode {
    SIMPLE,
    BREED_ONLY,
    POLLINATION,
    PIXEL_LAB
}

class PixelCatViewModel : ViewModel() {
    var isProcessing by mutableStateOf(false)
    var statusMessage by mutableStateOf("Ready")

    private val geminiApiKey = "AIzaSyCE8qhGhgWPKuQanP-ymsZDUxo-ElPEyQQ"
    private val pixelLabSecret = "7803f5a8-ab57-42a9-ba73-711fae9bfcce"
    private val removeBgApiKey = "pbVxUUqaWMLpBcG9Tj5qFAdu"
    private val pollinationsApiKey = "sk_WbnmIq8g9K1BVdo9GcGGdgxmPchgJZ3B"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    private val geminiModel = GenerativeModel(modelName = "gemini-3-flash-preview", apiKey = geminiApiKey)

    fun rescueCat(
        context: Context,
        originalBitmap: Bitmap,
        name: String,
        location: String,
        date: String,
        mode: RescueMode,
        onComplete: () -> Unit
    ) {
        if (isProcessing) return
        isProcessing = true
        statusMessage = "Identifying Resident..."

        viewModelScope.launch {
            try {
                // 1. Save Original Photo
                val originalPath = saveImageToInternalStorage(context, originalBitmap, "photo_${System.currentTimeMillis()}")

                // We only change this to TRUE if we successfully generate a sticker below.
                var canAddToRoom = false

                // Default sticker path is the original image (for Scrapbook)
                var finalStickerPath: String? = originalPath
                var detectedBreed = "Cute Cat"

                // 2. Identify Feline
                val aiBitmap = Bitmap.createScaledBitmap(originalBitmap, 512, 512, true)
                val response = try {
                    geminiModel.generateContent(content {
                        image(aiBitmap)
                        text("Is this a feline (domestic cat, lynx, tiger, lion, etc.)? If YES, return the specific species/breed in 2 words (e.g. 'Wild Lynx', 'Orange Tabby'). If NO, return 'REJECTED: [What is it?]'.")
                    })
                } catch (e: Exception) {
                    null
                }

                val resultText = response?.text?.trim() ?: "Cute Cat"

                // REJECTION LOGIC
                if (resultText.contains("REJECTED", ignoreCase = true)) {
                    val objectName = resultText.substringAfter(":").trim()
                    statusMessage = "Not a feline! 🐾"
                    isProcessing = false
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Wait! That looks like a $objectName! 🐶 This app is for felines only!", Toast.LENGTH_LONG).show()
                    }
                    File(originalPath).delete()
                    return@launch
                }

                detectedBreed = resultText

                // 3. Generate Sticker (ONLY if NOT Simple mode)
                if (mode != RescueMode.SIMPLE) {
                    statusMessage = "Designing Sticker..."
                    val processedBitmap: Bitmap? = when (mode) {
                        RescueMode.BREED_ONLY -> removeBackground(originalBitmap)
                        RescueMode.POLLINATION -> {
                            val pollinated = generatePollinationsArt(detectedBreed)
                            // If pollination fails, fallback to removing BG from original
                            if (pollinated != null) removeBackground(pollinated) else removeBackground(originalBitmap)
                        }
                        RescueMode.PIXEL_LAB -> {
                            val base64Sticker = generatePixelArt(aiBitmap, detectedBreed)
                            if (!base64Sticker.isNullOrEmpty()) {
                                val bytes = Base64.decode(base64Sticker, Base64.DEFAULT)
                                val pixelBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                if (pixelBitmap != null) removeBackground(pixelBitmap) else removeBackground(aiBitmap)
                            } else {
                                // Fallback if Pixel Lab fails
                                removeBackground(aiBitmap)
                            }
                        }
                        else -> null
                    }

                    if (processedBitmap != null) {
                        finalStickerPath = saveImageToInternalStorage(context, processedBitmap, "sticker_${System.currentTimeMillis()}")
                        canAddToRoom = true
                    }
                }

                // 4. Save to Database
                CatDatabase.getDatabase(context).catDao().insertCat(
                    CatItem(
                        name = name,
                        location = location,
                        date = date,
                        breed = detectedBreed,
                        imagePath = originalPath,
                        stickerPath = finalStickerPath,
                        isInRoom = canAddToRoom, // This uses the strict logic above
                        roomIndex = 0,
                        posX = 200f,
                        posY = 600f
                    )
                )

                withContext(Dispatchers.Main) {
                    statusMessage = "Welcome Home! 🛋️"
                    delay(800)
                    isProcessing = false
                    onComplete()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("PixelCat", "Error: ${e.message}")
                    statusMessage = "Connection Error"
                    isProcessing = false
                }
            }
        }
    }

    // Helper Functions

    private suspend fun generatePollinationsArt(breed: String): Bitmap? = withContext(Dispatchers.IO) {
        val prompt = "pixel art sticker of a $breed cat, cozy home lighting, white background, high quality 8-bit"
        val url = "https://image.pollinations.ai/prompt/${prompt.replace(" ", "%20")}"
        val request = Request.Builder().url(url).addHeader("Authorization", "Bearer $pollinationsApiKey").build()
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                if (bytes != null) BitmapFactory.decodeByteArray(bytes, 0, bytes.size) else null
            } else null
        } catch (e: Exception) { null }
    }

    private suspend fun generatePixelArt(reference: Bitmap, breed: String): String? = withContext(Dispatchers.IO) {
        val prompt = "A high-quality 8-bit pixel art sticker of a $breed cat, sitting pose, cozy interior lighting, white background, vector style."
        val json = JSONObject().apply {
            put("reference_image", bitmapToBase64(reference))
            put("description", prompt)
            put("image_size", JSONObject().apply { put("width", 128); put("height", 128) })
            put("guidance_scale", 10.0)
        }
        val request = Request.Builder()
            .url("https://api.pixellab.ai/v1/generate-image-bitforge")
            .addHeader("Authorization", "Bearer $pixelLabSecret")
            .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful && body != null) JSONObject(body).optJSONObject("image")?.optString("base64") else null
        } catch (e: Exception) { null }
    }

    private suspend fun removeBackground(bitmap: Bitmap): Bitmap? = withContext(Dispatchers.IO) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("image_file", "s.png", stream.toByteArray().toRequestBody("image/png".toMediaTypeOrNull()))
            .build()
        val request = Request.Builder().url("https://api.remove.bg/v1.0/removebg")
            .addHeader("X-Api-Key", removeBgApiKey).post(body).build()
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                if (bytes != null) BitmapFactory.decodeByteArray(bytes, 0, bytes.size) else null
            } else null
        } catch (e: Exception) { null }
    }

    private suspend fun saveImageToInternalStorage(context: Context, bitmap: Bitmap, name: String): String = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "$name.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        file.absolutePath
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 40, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    // This function prevents user from manually adding Simple cats to the room later
    fun toggleCatRoomStatus(context: Context, cat: CatItem, inRoom: Boolean) {
        if (cat.stickerPath == cat.imagePath) {
            viewModelScope.launch(Dispatchers.Main) {
                Toast.makeText(context, "Simple photos can't go in the Room! 🖼️", Toast.LENGTH_SHORT).show()
            }
            // Force it to remain false
            viewModelScope.launch(Dispatchers.IO) {
                CatDatabase.getDatabase(context).catDao().updateCat(cat.copy(isInRoom = false))
            }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            CatDatabase.getDatabase(context).catDao().updateCat(cat.copy(isInRoom = inRoom))
        }
    }

    fun deleteCatPermanently(context: Context, cat: CatItem) {
        viewModelScope.launch(Dispatchers.IO) {
            CatDatabase.getDatabase(context).catDao().deleteCat(cat)
        }
    }

    fun saveCatPosition(context: Context, cat: CatItem, x: Float, y: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            CatDatabase.getDatabase(context).catDao().updateCat(cat.copy(posX = x, posY = y))
        }
    }

    fun moveCatToRoom(context: Context, cat: CatItem, newRoomIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedCat = cat.copy(roomIndex = newRoomIndex)
            CatDatabase.getDatabase(context).catDao().updateCat(updatedCat)
        }
    }
}