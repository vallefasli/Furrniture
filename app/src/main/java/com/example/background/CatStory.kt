package com.example.background

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@Composable
fun CatStoryScreen(
    cats: List<CatItem>,
    pixelVm: PixelCatViewModel = viewModel()
) {
    val context = LocalContext.current
    var catToPermanentDelete by remember { mutableStateOf<CatItem?>(null) }

    // PERMANENT DELETE DIALOG
    if (catToPermanentDelete != null) {
        Dialog(onDismissRequest = { catToPermanentDelete = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CozyCream),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.padding(16.dp).shadow(20.dp, RoundedCornerShape(28.dp))
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Delete Forever?", color = CozyBrown, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Are you sure? This removes ${catToPermanentDelete?.name} from both the Scrapbook and Room permanently. 😿", color = CozyBrown.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = { catToPermanentDelete = null }) { Text("Cancel", color = CozyBrown) }
                        Button(
                            onClick = {
                                catToPermanentDelete?.let { pixelVm.deleteCatPermanently(context, it) }
                                catToPermanentDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            shape = RoundedCornerShape(50.dp)
                        ) { Text("Delete", color = Color.White) }
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(CozyCream).padding(top = 48.dp)) {
        Text("Cat Scrapbook 📔", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = CozyBrown, modifier = Modifier.padding(16.dp))

        if (cats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No cats rescued yet! 🐾", color = CozyBrown.copy(alpha = 0.5f))
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(12.dp)) {
                items(cats) { cat ->
                    Card(
                        modifier = Modifier.padding(8.dp).shadow(4.dp, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                            AsyncImage(model = cat.imagePath, contentDescription = null, modifier = Modifier.size(120.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(cat.name, fontWeight = FontWeight.Bold, color = CozyBrown)

                            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                // BUTTON 1: HOME (RE-ADD TO ROOM)
                                IconButton(
                                    onClick = { pixelVm.toggleCatRoomStatus(context, cat, !cat.isInRoom) },
                                    modifier = Modifier.background(if (cat.isInRoom) CozyPeach.copy(alpha = 0.3f) else CozyCoral.copy(alpha = 0.1f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (cat.isInRoom) Icons.Default.Home else Icons.Default.Add,
                                        contentDescription = "Room Status",
                                        tint = if (cat.isInRoom) CozyBrown else CozyCoral
                                    )
                                }
                                // BUTTON 2: TRASH (PERMANENT DELETE)
                                IconButton(
                                    onClick = { catToPermanentDelete = cat },
                                    modifier = Modifier.background(Color.Red.copy(alpha = 0.1f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}