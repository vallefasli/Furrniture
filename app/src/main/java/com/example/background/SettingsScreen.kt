package com.example.background

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onDeleteAllCats: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("story_kitty_prefs", Context.MODE_PRIVATE)

    // Load saved settings
    var isMusicEnabled by remember { mutableStateOf(prefs.getBoolean("music_enabled", true)) }
    var isCreativeMode by remember { mutableStateOf(prefs.getBoolean("creative_mode", false)) }

    // For Delete Confirmation
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CozyCream)
            .padding(24.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = CozyBrown)
            }
            Text(
                "Settings ⚙️",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = CozyBrown,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 1. MUSIC TOGGLE (Moved here from Cat Room!)
        SettingsCard(title = "Audio") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Background Music", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CozyBrown)
                    Text("Play relaxing tunes in Cat Room", fontSize = 14.sp, color = Color.Gray)
                }
                Switch(
                    checked = isMusicEnabled,
                    onCheckedChange = {
                        isMusicEnabled = it
                        prefs.edit().putBoolean("music_enabled", it).apply()
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = CozyCoral, checkedTrackColor = CozyPeach)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. CREATIVE MODE (Unlock Rooms)
        SettingsCard(title = "Gameplay") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Creative Mode", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CozyBrown)
                    Text("Unlock all 5 rooms instantly", fontSize = 14.sp, color = Color.Gray)
                }
                Switch(
                    checked = isCreativeMode,
                    onCheckedChange = {
                        isCreativeMode = it
                        prefs.edit().putBoolean("creative_mode", it).apply()
                        Toast.makeText(context, if(it) "All Rooms Unlocked! 🔓" else "Standard Progression Restored", Toast.LENGTH_SHORT).show()
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = CozyCoral, checkedTrackColor = CozyPeach)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 3. DANGER ZONE (Delete Data)
        Text("Danger Zone", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Red, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
        Button(
            onClick = { showDeleteDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)), // Soft Red
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reset Game Progress", fontSize = 16.sp, color = Color.White)
        }
    }

    // DELETE CONFIRMATION POPUP
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Reset Everything?", fontWeight = FontWeight.Bold, color = CozyBrown) },
            text = { Text("This will delete ALL your rescued cats and scrapbook photos. This cannot be undone!", color = Color.Gray) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAllCats()
                        showDeleteDialog = false
                        Toast.makeText(context, "Game Reset Complete.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Yes, Delete All", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel", color = CozyBrown) }
            },
            containerColor = CozyCream
        )
    }
}

@Composable
fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}