package com.example.background
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
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

// ✨ NEW: Define the different rescue modes
enum class RescueMode {
    SIMPLE,
    BREED_ONLY,
    POLLINATION,
    PIXEL_LAB
}

class PixelCatViewModel : ViewModel() {
    var isProcessing by mutableStateOf(false)
    var statusMessage by mutableStateOf("Ready")

    private val geminiApiKey = "AIzaSyAKTsYbzGg9_sd5aFlpeKQVO5bx6xu1q78"

    private val pixelLabSecret = "4eae6276-1908-4989-8b5e-cb9499ad15e0"
    private val removeBgApiKey = "ezQZ4qm1YM2obsw5ULwx57Fb"
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
        mode: RescueMode, // ✨ Accepts the mode
        onComplete: () -> Unit
    ) {
        if (isProcessing) return
        isProcessing = true
        statusMessage = "Analyzing Cat..."

        viewModelScope.launch {
            try {
                val originalPath = saveImageToInternalStorage(context, originalBitmap, "photo_${System.currentTimeMillis()}")
                var detectedBreed = "Cute Cat"
                var finalStickerPath: String? = originalPath
                var canAddToRoom = true

                if (mode != RescueMode.SIMPLE) {
                    val aiBitmap = Bitmap.createScaledBitmap(originalBitmap, 512, 512, true)
                    val response = geminiModel.generateContent(content {
                        image(aiBitmap)
                        text("Identify this cat's color and breed. Reply with ONLY 2 words.")
                    })
                    detectedBreed = response.text?.trim() ?: "Cute Cat"

                    statusMessage = "Processing Image..."
                    val processedBitmap: Bitmap? = when (mode) {
                        RescueMode.BREED_ONLY -> {
                            removeBackground(originalBitmap)
                        }
                        RescueMode.POLLINATION -> {
                            val pollinated = generatePollinationsArt(detectedBreed)
                            if (pollinated != null) removeBackground(pollinated) else removeBackground(originalBitmap)
                        }
                        RescueMode.PIXEL_LAB -> {
                            val base64Sticker = generatePixelArt(aiBitmap, detectedBreed)
                            if (!base64Sticker.isNullOrEmpty()) {
                                val bytes = Base64.decode(base64Sticker, Base64.DEFAULT)
                                val pixelBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                if (pixelBitmap != null) removeBackground(pixelBitmap) else removeBackground(aiBitmap)
                            } else removeBackground(aiBitmap)
                        }
                        else -> null
                    }

                    processedBitmap?.let {
                        finalStickerPath = saveImageToInternalStorage(context, it, "sticker_${System.currentTimeMillis()}")
                    }
                } else {
                    canAddToRoom = false
                }

                CatDatabase.getDatabase(context).catDao().insertCat(
                    CatItem(
                        name = name,
                        location = location,
                        date = date,
                        breed = if (mode == RescueMode.SIMPLE) "Scrapbook Only" else detectedBreed,
                        imagePath = originalPath,
                        stickerPath = finalStickerPath,
                        isInRoom = canAddToRoom
                    )
                )

                statusMessage = "Rescued! ✨"
                onComplete()
            } catch (e: Exception) {
                Log.e("PixelCat", "Error: ${e.message}")
                statusMessage = "Error"
            } finally {
                isProcessing = false
            }
        }
    }

    private suspend fun generatePollinationsArt(breed: String): Bitmap? = withContext(Dispatchers.IO) {
        val prompt = "pixel art sticker of a $breed cat, white background, high quality 8-bit"
        val url = "https://image.pollinations.ai/prompt/${prompt.replace(" ", "%20")}"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $pollinationsApiKey")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                if (bytes != null) BitmapFactory.decodeByteArray(bytes, 0, bytes.size) else null
            } else null
        } catch (e: Exception) { null }
    }

    private suspend fun generatePixelArt(reference: Bitmap, breed: String): String? = withContext(Dispatchers.IO) {
        val prompt = "A high-quality 8-bit pixel art sticker of a $breed cat, full body, white background, vector style."
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

    fun toggleCatRoomStatus(context: Context, cat: CatItem, inRoom: Boolean) {
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
}