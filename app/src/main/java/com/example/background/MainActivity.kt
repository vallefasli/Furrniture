package com.example.background

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StoryKittyApp() }
    }
}

@Composable
fun StoryKittyApp() {
    val context = LocalContext.current
    val selectedScreen = remember { mutableStateOf<Screen>(CameraScreen) }
    val db = remember { CatDatabase.getDatabase(context) }
    val catList by db.catDao().getAllCats().collectAsState(initial = emptyList())
    val pixelVm: PixelCatViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val navItems = listOf(
        NavItem("Home", R.drawable.logohome, R.drawable.logohome, CameraScreen),
        NavItem("Rescue", R.drawable.logocamera, R.drawable.logocamera, AddCatScreen),
        NavItem("Residents", R.drawable.logopet, R.drawable.logopet, CollectionScreen), // <--- CHANGED THIS
        NavItem("Rooms", R.drawable.logolocation, R.drawable.logolocation, CatStoryScreen),
    )

    MaterialTheme {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (selectedScreen.value !is SettingsScreen) {
                    InstagramBottomBar(
                        items = navItems,
                        selected = selectedScreen.value,
                        onSelect = { selectedScreen.value = it }
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                when (selectedScreen.value) {
                    is CameraScreen -> HomeScreen(
                        catList = catList,
                        onNavigateToSettings = { selectedScreen.value = SettingsScreen }
                    )
                    is CollectionScreen -> CollectionScreen(catList, pixelVm)
                    is AddCatScreen -> AddCatScreen(onCatSaved = { selectedScreen.value = CollectionScreen })
                    is CatStoryScreen -> CatRoomScreen(cats = catList, pixelVm = pixelVm)
                    is SettingsScreen -> SettingsScreen(
                        onBack = { selectedScreen.value = CameraScreen },
                        onDeleteAllCats = {
                            scope.launch(Dispatchers.IO) {
                                db.clearAllTables()
                                withContext(Dispatchers.Main) { selectedScreen.value = CameraScreen }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(catList: List<CatItem>, onNavigateToSettings: () -> Unit) {
    val activeCats = catList.filter { it.isInRoom }
    var currentCatIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(activeCats.size) {
        while (activeCats.isNotEmpty()) {
            delay(6000L)
            currentCatIndex = (currentCatIndex + 1) % activeCats.size
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.homescreen),
            contentDescription = "Furr-niture",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Image(
            painter = painterResource(id = R.drawable.furr_niture),
            contentDescription = "App Title Logo",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 50.dp)
                .fillMaxWidth(0.8f)
                .height(170.dp),
            contentScale = ContentScale.Fit
        )


        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp)
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape)
        ) {
            Icon(Icons.Filled.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (99.dp), y = 120.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                "RESIDENTS: ${activeCats.size}",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.shadow(1.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))

            if (activeCats.isEmpty()) {
                Text("   House is empty", color = Color.White.copy(alpha = 0.8f), fontSize = 7.sp)
            } else {
                Text(
                    "• ${activeCats[currentCatIndex].name}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun CollectionScreen(cats: List<CatItem>, pixelVm: PixelCatViewModel) {
    val context = LocalContext.current
    var catToDeleteByPermanent by remember { mutableStateOf<CatItem?>(null) }

    if (catToDeleteByPermanent != null) {
        Dialog(onDismissRequest = { catToDeleteByPermanent = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CozyCream),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.padding(16.dp).shadow(20.dp, RoundedCornerShape(28.dp))
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Evict Resident?", color = CozyBrown, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("This removes ${catToDeleteByPermanent?.name} permanently.", color = CozyBrown.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = { catToDeleteByPermanent = null }) { Text("Cancel", color = CozyBrown) }
                        Button(
                            onClick = {
                                catToDeleteByPermanent?.let { pixelVm.deleteCatPermanently(context, it) }
                                catToDeleteByPermanent = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            shape = RoundedCornerShape(50.dp)
                        ) { Text("Goodbye", color = Color.White) }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(CozyCream)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Meow-ments!", fontSize = 60.sp, fontWeight = FontWeight.Bold, color = CozyBrown, fontFamily = FontFamily.Cursive, modifier = Modifier.padding(bottom = 24.dp, top = 8.dp).align(Alignment.CenterHorizontally))

            if (cats.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("House is empty... \nGo find some roommates! 🐈", color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(bottom = 80.dp, start = 4.dp, end = 4.dp)
                ) {
                    items(cats) { cat ->
                        ScrapbookItem(cat = cat, onToggleRoom = { pixelVm.toggleCatRoomStatus(context, cat, !cat.isInRoom) }, onDeletePermanently = { catToDeleteByPermanent = cat })
                    }
                }
            }
        }
    }
}

@Composable
fun ScrapbookItem(cat: CatItem, onToggleRoom: () -> Unit, onDeletePermanently: () -> Unit) {
    val rotation = remember(cat.id) { (cat.id % 10).toFloat() - 5f }
    val isRoomReady = cat.stickerPath != cat.imagePath

    Box(modifier = Modifier.fillMaxWidth().rotate(rotation)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, start = 8.dp, end = 8.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.aspectRatio(1f).fillMaxWidth().padding(10.dp).background(WallPaint)) {
                    AsyncImage(model = cat.imagePath, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Column(modifier = Modifier.padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = cat.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = CozyBrown, fontFamily = FontFamily.Cursive)
                    Text(text = cat.breed ?: "Unknown Cat", fontSize = 13.sp, color = CozyCoral, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Place, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = cat.location, fontSize = 13.sp, color = Color.Gray)
                        }
                        Text(text = cat.date, fontSize = 12.sp, color = Color.LightGray)
                    }
                }
                HorizontalDivider(color = CozyCream, thickness = 1.dp, modifier = Modifier.padding(top = 6.dp, start = 12.dp, end = 12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDeletePermanently, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    }

                    if (isRoomReady) {
                        TextButton(
                            onClick = onToggleRoom,
                            contentPadding = PaddingValues(0.dp),
                            // ✨ CHANGE HERE: Added padding to move it left
                            modifier = Modifier.padding(end = 45.dp)
                        ) {
                            Text(
                                if (cat.isInRoom) "🏠 In Room" else "💤 Sleeping",
                                color = if (cat.isInRoom) CozyBrown else CozyCoral,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text("Scrapbook Only", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.padding(end = 16.dp))
                    }
                }
            }
        }
        Box(modifier = Modifier.align(Alignment.TopCenter).width(50.dp).height(16.dp).background(CozyPeach.copy(alpha = 0.6f)))
        if (cat.stickerPath != null && isRoomReady) {
            AsyncImage(
                model = cat.stickerPath,
                contentDescription = "Sticker",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 6.dp, y = 6.dp)
                    .size(48.dp)
                    .rotate(-10f)
                    .shadow(2.dp, CircleShape)
                    .background(Color.White, CircleShape)
                    .padding(2.dp)
            )
        }
    }
}

@Composable
fun InstagramBottomBar(items: List<NavItem>, selected: Screen, onSelect: (Screen) -> Unit) {
    Column {
        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)

        NavigationBar(
            containerColor = CozyCream,
            tonalElevation = 0.dp, modifier = Modifier.height(65.dp)
        ) {
            items.forEach { item ->
                val isSelected = item.screen == selected
                val iconData = if (isSelected) item.selectedIcon else item.unselectedIcon

                NavigationBarItem(
                    icon = {
                        when (iconData) {
                            is ImageVector -> {
                                Icon(
                                    imageVector = iconData,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            is Int -> {
                                Icon(
                                    painter = painterResource(id = iconData),
                                    contentDescription = item.title,
                                    modifier = Modifier.size(26.dp),
                                    tint = Color.Unspecified
                                )
                            }
                        }
                    },
                    selected = isSelected,
                    onClick = { onSelect(item.screen) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent,
                        selectedIconColor = WoodDark,
                        unselectedIconColor = Color.Gray.copy(alpha = 0.6f)
                    ),
                    label = null
                )
            }
        }
    }
}