package com.example.background

import android.content.Context
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
fun SettingsScreen(onBack: () -> Unit, onDeleteAllCats: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("story_kitty_prefs", Context.MODE_PRIVATE) }

    var musicEnabled by remember { mutableStateOf(prefs.getBoolean("music_enabled", true)) }
    var creativeMode by remember { mutableStateOf(prefs.getBoolean("creative_mode", false)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CozyCream)
            .padding(24.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = CozyBrown)
            }
            Text(
                "Settings ⚙️",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = CozyBrown
            )
        }

        // --- Audio Section ---
        SettingsSectionHeader("Audio")
        SettingsCard {
            SettingsToggle(
                title = "Background Music",
                description = "Play relaxing tunes in Cat Room",
                checked = musicEnabled,
                onCheckedChange = {
                    musicEnabled = it
                    prefs.edit().putBoolean("music_enabled", it).apply()
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Gameplay Section ---
        SettingsSectionHeader("Gameplay")
        SettingsCard {
            SettingsToggle(
                title = "Creative Mode",
                description = "Unlock all 5 rooms instantly",
                checked = creativeMode,
                onCheckedChange = {
                    creativeMode = it
                    prefs.edit().putBoolean("creative_mode", it).apply()
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // --- Danger Zone ---
        Text(
            "Danger Zone",
            color = Color(0xFFD32F2F),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Button(
            onClick = onDeleteAllCats,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF9A9A)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFD32F2F))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reset Game Progress", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        content = content
    )
}

@Composable
fun SettingsToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CozyBrown)
            Text(description, fontSize = 14.sp, color = Color.Gray)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CozyCoral,
                checkedTrackColor = CozyPeach,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.LightGray
            )
        )
    }
}