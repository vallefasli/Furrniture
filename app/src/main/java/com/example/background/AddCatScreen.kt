package com.example.background

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCatScreen(
    onCatSaved: () -> Unit,
    pixelVm: PixelCatViewModel = viewModel()
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedMode by remember { mutableStateOf(RescueMode.PIXEL_LAB) }

    val date = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date()) }

    // Media Launchers
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { takenBitmap ->
        if (takenBitmap != null) {
            bitmap = takenBitmap
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permission Granted! Try clicking the button again.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permission Denied. This feature requires access to work.", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Resident", fontSize = 30.sp, color = CozyBrown, fontWeight = FontWeight.Bold) },
                // Kept your original Transparent TopAppBar with no Back Arrow
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = CozyCream
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Image Preview
            Card(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .shadow(8.dp, RoundedCornerShape(24.dp))
                    .then(
                        if (bitmap != null) {
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(bitmap!!.width.toFloat() / bitmap!!.height.toFloat())
                        } else {
                            Modifier.size(220.dp)
                        }
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    if (bitmap != null) {
                        AsyncImage(
                            model = bitmap,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text("No Photo", color = CozyBrown.copy(alpha = 0.3f), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        // Check if device even has a camera first
                        val hasCamera = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
                        if (!hasCamera) {
                            Toast.makeText(context, "No camera found on this device", Toast.LENGTH_SHORT).show()
                        } else {
                            val status = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                            if (status == PackageManager.PERMISSION_GRANTED) {
                                cameraLauncher.launch()
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = CozyPeach),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Take Photo", color = CozyBrown)
                }

                Button(
                    onClick = {
                        val galleryPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Manifest.permission.READ_MEDIA_IMAGES
                        } else {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        }

                        val status = ContextCompat.checkSelfPermission(context, galleryPermission)
                        if (status == PackageManager.PERMISSION_GRANTED) {
                            galleryLauncher.launch("image/*")
                        } else {
                            permissionLauncher.launch(galleryPermission)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = CozyPeach),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Open Gallery", color = CozyBrown)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Select Method",
                modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp),
                fontWeight = FontWeight.Bold,
                color = CozyBrown
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RescueOptionCard(
                    title = "High Quality",
                    description = "8-bit art. Fits Room perfectly.",
                    isSelected = selectedMode == RescueMode.PIXEL_LAB,
                    onClick = { selectedMode = RescueMode.PIXEL_LAB }
                )
                RescueOptionCard(
                    title = "Simple",
                    description = "Adds to meow-ments only (Cannot add to Room).",
                    isSelected = selectedMode == RescueMode.SIMPLE,
                    onClick = { selectedMode = RescueMode.SIMPLE }
                )
                RescueOptionCard(
                    title = "Removes BG",
                    description = "Removes background only. May not match Room aesthetic.",
                    isSelected = selectedMode == RescueMode.BREED_ONLY,
                    onClick = { selectedMode = RescueMode.BREED_ONLY }
                )
                RescueOptionCard(
                    title = "Experimental",
                    description = "Experimental AI art. Results may be inconsistent.",
                    isSelected = selectedMode == RescueMode.POLLINATION,
                    onClick = { selectedMode = RescueMode.POLLINATION }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Kitty Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CozyCoral,
                            unfocusedBorderColor = CozyPeach,
                            focusedLabelColor = CozyCoral
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Found At (Location)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CozyCoral,
                            unfocusedBorderColor = CozyPeach,
                            focusedLabelColor = CozyCoral
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    bitmap?.let {
                        pixelVm.rescueCat(context, it, name, location, date, selectedMode) {
                            onCatSaved()
                        }
                    }
                },
                enabled = name.isNotEmpty() && location.isNotEmpty() && bitmap != null && !pixelVm.isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = CozyCoral),
                shape = RoundedCornerShape(20.dp)
            ) {
                if (pixelVm.isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(pixelVm.statusMessage)
                } else {
                    Text("Complete Rescue", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun RescueOptionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(2.dp, CozyCoral) else null,
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isSelected) CozyPeach.copy(alpha = 0.2f) else Color.Transparent)
                .padding(12.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold, color = CozyBrown)
            Text(description, fontSize = 11.sp, color = Color.Gray)
        }
    }
}