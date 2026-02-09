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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddCatScreen(
    onCatSaved: () -> Unit,
    viewModel: PixelCatViewModel = viewModel()
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedUri = uri }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).background(Color(0xFFFDFCF0)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Rescue Cat", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4E342E), modifier = Modifier.padding(vertical = 16.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White, RoundedCornerShape(12.dp))
                .clickable { if (!viewModel.isProcessing) launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (selectedUri != null) {
                Image(painter = rememberAsyncImagePainter(selectedUri), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text("📸 Tap to add photo", fontSize = 18.sp, color = Color.Gray)
            }

            if (viewModel.isProcessing) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFFF8A65))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(viewModel.statusMessage, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (name.isNotEmpty() && selectedUri != null) {
                    try {
                        // ✨ FIX: Use .use { } to auto-close the stream (Fixes "Resource failed to call close")
                        val bitmap = context.contentResolver.openInputStream(selectedUri!!)?.use { inputStream ->
                            BitmapFactory.decodeStream(inputStream)
                        }

                        if (bitmap != null) {
                            val today = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date())
                            viewModel.rescueCat(context, bitmap, name, location, today) {
                                onCatSaved()
                            }
                        } else {
                            Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Add Name & Photo!", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = !viewModel.isProcessing,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8A65))
        ) {
            Text(if (viewModel.isProcessing) "Processing..." else "SAVE CAT 🐾", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}