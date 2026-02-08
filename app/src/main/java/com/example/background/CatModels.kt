package com.example.background

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cats")
data class CatItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val location: String,
    val date: String,
    val breed: String? = null,
    val imagePath: String? = null,
    val stickerPath: String? = null,
    val posX: Float = 0f,
    val posY: Float = 0f
)

data class NavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val screen: Screen
)

sealed class Screen
object CameraScreen : Screen()
object CollectionScreen : Screen()
object AddCatScreen : Screen()
object CatStoryScreen : Screen()
object SettingsScreen : Screen() // ✨ Added this!