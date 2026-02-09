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

    // ✨ LOAD SETTINGS (Creative Mode & Music)
    val prefs = context.getSharedPreferences("story_kitty_prefs", android.content.Context.MODE_PRIVATE)
    val isCreativeMode = prefs.getBoolean("creative_mode", false)
    val isMusicEnabled = prefs.getBoolean("music_enabled", true)

    // CONFIGURATION
    val catsPerRoom = 6
    val maxRooms = 5

    // ✨ LOGIC: If Creative Mode is ON, unlock everything (5). Otherwise calculate normally.
    val unlockedCount = if (isCreativeMode) maxRooms else min(maxRooms, (cats.size / catsPerRoom) + 1)

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

    val rooms = remember(cats) {
        val chunked = cats.chunked(catsPerRoom)
        List(maxRooms) { index -> chunked.getOrElse(index) { emptyList() } }
    }

    val pagerState = rememberPagerState(pageCount = { maxRooms })
    var isUiVisible by remember { mutableStateOf(true) }

    val mediaPlayer = remember {
        try { MediaPlayer.create(context, R.raw.cat_music)?.apply { isLooping = true } } catch (e: Exception) { null }
    }

    DisposableEffect(Unit) {
        mediaPlayer?.start()
        onDispose { mediaPlayer?.stop(); mediaPlayer?.release() }
    }

    // ✨ MUSIC LOGIC: Controlled ONLY by Settings now!
    LaunchedEffect(isMusicEnabled) {
        if (!isMusicEnabled) {
            mediaPlayer?.setVolume(0f, 0f)
        } else {
            mediaPlayer?.setVolume(1f, 1f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { isUiVisible = !isUiVisible }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
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
                            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                            modifier = Modifier.padding(32.dp).shadow(16.dp, RoundedCornerShape(24.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Rounded.Lock, contentDescription = "Locked", tint = CozyCoral, modifier = Modifier.size(56.dp).background(CozyPeach.copy(alpha = 0.3f), CircleShape).padding(12.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Room ${pageIndex + 1} Locked", color = CozyBrown, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("You need to rescue more cats! \nFill the previous room with ${catsPerRoom} cats to unlock this cozy space.", color = CozyBrown.copy(alpha = 0.7f), textAlign = TextAlign.Center, fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    if (roomCats.isEmpty()) {
                        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Card(colors = CardDefaults.cardColors(containerColor = CozyCream.copy(alpha = 0.9f)), shape = RoundedCornerShape(16.dp)) {
                                Text("Room Ready! 🐾", color = CozyBrown, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                            }
                        }
                    } else {
                        roomCats.forEachIndexed { indexInRoom, cat ->
                            val col = indexInRoom % 3
                            val row = indexInRoom / 3
                            val defaultX = (col * 110f) + 40f
                            val defaultY = 500f + (row * 120f)
                            val initialPos = if (cat.posX == 0f && cat.posY == 0f) Offset(defaultX, defaultY) else Offset(cat.posX, cat.posY)

                            DraggableCatSticker(
                                cat = cat,
                                initialPosition = initialPos,
                                onDragEnd = { finalPos -> pixelVm.saveCatPosition(context, cat, finalPos.x, finalPos.y) },
                                showName = isUiVisible
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = isUiVisible, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(color = CozyCream.copy(alpha = 0.9f), shape = RoundedCornerShape(50), shadowElevation = 4.dp) {
                        Text(text = "  🏠 Room ${pagerState.currentPage + 1}  ", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CozyBrown)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.background(Color.Black.copy(alpha = 0.2f), CircleShape).padding(4.dp)) {
                        repeat(maxRooms) { iteration ->
                            val isLocked = (iteration + 1) > unlockedCount
                            val color = if (pagerState.currentPage == iteration) CozyCoral else if (isLocked) Color.Gray else Color.White
                            Box(modifier = Modifier.padding(4.dp).clip(CircleShape).background(color).size(8.dp))
                        }
                    }
                }

                // ✨ AUDIO BUTTON REMOVED FROM HERE

                if (pagerState.currentPage > 0) {
                    IconButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }, modifier = Modifier.align(Alignment.CenterStart).padding(16.dp).size(56.dp).background(CozyBrown, CircleShape).shadow(8.dp, CircleShape)) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Prev", tint = Color.White)
                    }
                }
                if (pagerState.currentPage < maxRooms - 1) {
                    IconButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }, modifier = Modifier.align(Alignment.CenterEnd).padding(16.dp).size(56.dp).background(CozyBrown, CircleShape).shadow(8.dp, CircleShape)) {
                        Icon(Icons.Filled.ArrowForward, contentDescription = "Next", tint = Color.White)
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
        val path = cat.stickerPath ?: cat.imagePath
        AsyncImage(model = path, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        if (showName) {
            Text(text = cat.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CozyBrown, modifier = Modifier.align(Alignment.TopCenter).offset(y = (-15).dp).background(CozyCream.copy(alpha = 0.9f), shape = RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp).shadow(2.dp, RoundedCornerShape(4.dp)))
        }
    }
}