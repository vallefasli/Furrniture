package com.example.background

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import java.io.InputStream

@Composable
fun AddCatScreen(
    onCatSaved: () -> Unit,
    // ✨ FIX: Using PixelCatViewModel properly
    viewModel: PixelCatViewModel = viewModel()
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color(0xFFF5F5F5)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "New Pixel Cat",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp),
            color = Color(0xFF333333)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(Color.White, shape = RoundedCornerShape(12.dp))
                .clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (viewModel.generatedSticker != null) {
                Image(
                    bitmap = viewModel.generatedSticker!!.asImageBitmap(),
                    contentDescription = "Sticker",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else if (selectedUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(selectedUri),
                    contentDescription = "Selected Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text("Tap to select photo", color = Color.Gray)
            }

            if (viewModel.isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(viewModel.statusMessage, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (selectedUri != null && !viewModel.isProcessing) {
                    try {
                        val inputStream: InputStream? = context.contentResolver.openInputStream(selectedUri!!)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        viewModel.identifyAndPixelate(context, bitmap) { }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Image Error", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Select an image first!", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = !viewModel.isProcessing && selectedUri != null,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE)),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("✨ Generate Pixel Sticker")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Cat Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth()
        )

        if (viewModel.detectedBreed != null) {
            Text(
                text = "Detected: ${viewModel.detectedBreed}",
                color = Color(0xFF6200EE),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp).align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (name.isNotEmpty() && location.isNotEmpty()) {
                    val inputStream = if (selectedUri != null) context.contentResolver.openInputStream(selectedUri!!) else null
                    val originalBitmap = if (inputStream != null) BitmapFactory.decodeStream(inputStream) else null
                    viewModel.saveCatToDatabase(context, name, location, "Today", originalBitmap)
                    onCatSaved()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5))
        ) {
            Text("Save Cat", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}