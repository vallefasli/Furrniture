package com.example.background

import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun CatRoomScreen(
    cats: List<CatItem>,
    pixelVm: PixelCatViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ✨ FILTER: Only show cats currently marked for the room
    val activeCats = remember(cats) { cats.filter { it.isInRoom } }

    val prefs = context.getSharedPreferences("story_kitty_prefs", android.content.Context.MODE_PRIVATE)
    val isCreativeMode = prefs.getBoolean("creative_mode", false)
    val isMusicEnabled = prefs.getBoolean("music_enabled", true)

    val catsPerRoom = 6
    val maxRooms = 5
    val unlockedCount = if (isCreativeMode) maxRooms else min(maxRooms, (activeCats.size / catsPerRoom) + 1)

    val roomBackgrounds = listOf(
        R.drawable.livingroom, R.drawable.bedroom, R.drawable.garden, R.drawable.rooftop, R.drawable.basement
    )

    val roomTints = listOf(
        ColorMatrix(),
        ColorMatrix().apply { setToSaturation(0f) },
        ColorMatrix().apply { setToScale(1f, 0.9f, 0.8f, 1f) },
        ColorMatrix().apply { setToScale(0.8f, 0.9f, 1f, 1f) },
        ColorMatrix().apply { setToScale(0.9f, 1f, 0.9f, 1f) }
    )

    val rooms = remember(activeCats) {
        val chunked = activeCats.chunked(catsPerRoom)
        List(maxRooms) { index -> chunked.getOrElse(index) { emptyList() } }
    }

    val pagerState = rememberPagerState(pageCount = { maxRooms })
    var isUiVisible by remember { mutableStateOf(true) }

    var isDeleteMode by remember { mutableStateOf(false) }
    var catToHide by remember { mutableStateOf<CatItem?>(null) }

    val mediaPlayer = remember {
        try { MediaPlayer.create(context, R.raw.cat_music)?.apply { isLooping = true } } catch (e: Exception) { null }
    }

    DisposableEffect(Unit) {
        mediaPlayer?.start()
        onDispose { mediaPlayer?.stop(); mediaPlayer?.release() }
    }

    LaunchedEffect(isMusicEnabled) {
        if (!isMusicEnabled) mediaPlayer?.setVolume(0f, 0f) else mediaPlayer?.setVolume(1f, 1f)
    }

    // ✨ SOFT REMOVAL DIALOG
    if (catToHide != null) {
        Dialog(onDismissRequest = { catToHide = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CozyCream),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.padding(16.dp).shadow(20.dp, RoundedCornerShape(28.dp))
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Remove from Room?", color = CozyBrown, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("${catToHide?.name} will stay in your Scrapbook, but won't be in the room for now. 🐾", color = CozyBrown.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = { catToHide = null }) { Text("Cancel", color = CozyBrown) }
                        Button(
                            onClick = {
                                catToHide?.let { pixelVm.toggleCatRoomStatus(context, it, false) }
                                catToHide = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CozyCoral),
                            shape = RoundedCornerShape(50.dp)
                        ) { Text("Hide Cat", color = Color.White) }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black).clickable { if (!isDeleteMode) isUiVisible = !isUiVisible }
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
            val roomCats = rooms.getOrElse(pageIndex) { emptyList() }
            val bgImage = roomBackgrounds.getOrElse(pageIndex) { R.drawable.catssy }
            val colorMatrix = roomTints.getOrElse(pageIndex) { ColorMatrix() }
            val isLocked = (pageIndex + 1) > unlockedCount

            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = bgImage),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = if (isLocked) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f); setToScale(0.6f, 0.6f, 0.6f, 1f) }) else ColorFilter.colorMatrix(colorMatrix)
                )

                if (isLocked) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CozyCream),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.padding(32.dp).shadow(16.dp, RoundedCornerShape(24.dp))
                        ) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.Lock, contentDescription = null, tint = CozyCoral, modifier = Modifier.size(56.dp).background(CozyPeach.copy(alpha = 0.3f), CircleShape).padding(12.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Room ${pageIndex + 1} Locked", color = CozyBrown, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Text("Rescue more cats to unlock!", color = CozyBrown.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    roomCats.forEachIndexed { indexInRoom, cat ->
                        val col = indexInRoom % 3
                        val row = indexInRoom / 3
                        val defaultX = (col * 110f) + 40f
                        val defaultY = 500f + (row * 120f)
                        val initialPos = if (cat.posX == 0f && cat.posY == 0f) Offset(defaultX, defaultY) else Offset(cat.posX, cat.posY)

                        Box {
                            DraggableCatSticker(
                                cat = cat,
                                initialPosition = initialPos,
                                onDragEnd = { finalPos -> pixelVm.saveCatPosition(context, cat, finalPos.x, finalPos.y) },
                                showName = isUiVisible
                            )

                            if (isDeleteMode && isUiVisible) {
                                Box(
                                    modifier = Modifier
                                        .offset { IntOffset(initialPos.x.roundToInt() + 90, initialPos.y.roundToInt() - 10) }
                                        .size(32.dp)
                                        .background(CozyCoral, CircleShape)
                                        .shadow(4.dp, CircleShape)
                                        .clickable { catToHide = cat },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Hide", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = isUiVisible, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(color = CozyCream.copy(alpha = 0.95f), shape = RoundedCornerShape(50), shadowElevation = 6.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(text = "🏠 Room ${pagerState.currentPage + 1}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CozyBrown)
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(
                                modifier = Modifier.size(32.dp).background(if (isDeleteMode) CozyCoral else CozyPeach.copy(alpha = 0.6f), CircleShape).clickable { isDeleteMode = !isDeleteMode },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = if (isDeleteMode) Icons.Default.Done else Icons.Default.Delete, contentDescription = null, tint = if (isDeleteMode) Color.White else CozyBrown, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                if (pagerState.currentPage > 0) {
                    IconButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }, modifier = Modifier.align(Alignment.CenterStart).padding(16.dp).size(56.dp).background(CozyBrown, CircleShape).shadow(8.dp, CircleShape)) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                }
                if (pagerState.currentPage < maxRooms - 1) {
                    IconButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }, modifier = Modifier.align(Alignment.CenterEnd).padding(16.dp).size(56.dp).background(CozyBrown, CircleShape).shadow(8.dp, CircleShape)) {
                        Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun DraggableCatSticker(cat: CatItem, initialPosition: Offset, onDragEnd: (Offset) -> Unit, showName: Boolean) {
    var offset by remember { mutableStateOf(initialPosition) }
    Box(
        modifier = Modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .pointerInput(Unit) { detectDragGestures(onDragEnd = { onDragEnd(offset) }) { change, dragAmount -> change.consume(); offset += dragAmount } }
            .size(128.dp)
    ) {
        val path = cat.stickerPath ?: cat.imagePath ?: ""
        AsyncImage(model = path, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        if (showName) {
            Text(text = cat.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CozyBrown, modifier = Modifier.align(Alignment.TopCenter).offset(y = (-15).dp).background(CozyCream.copy(alpha = 0.9f), shape = RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp).shadow(2.dp, RoundedCornerShape(4.dp)))
        }
    }
}