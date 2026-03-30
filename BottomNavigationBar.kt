package com.hanif.nagarfix

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.map

@Composable
fun BottomNavigationBar(activity: Activity) {
    val context = LocalContext.current
    val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    val darkMode by context.dataStore.data.map { it[DARK_MODE_KEY] ?: false }.collectAsState(initial = false)

    NavigationBar(
        containerColor = if (darkMode) Color(0xFF1E1E1E) else Color.White,
        contentColor = if (darkMode) Color(0xFF81C784) else Color(0xFF2E7D32)
    ) {
        val navItems = listOf(
            Triple("Home", Icons.Default.Home, HomeActivity::class.java),
            Triple("Post", Icons.Default.AddCircle, AddPostActivity::class.java),
            Triple("Reports", Icons.Default.Notifications, NotificationActivity::class.java),
            Triple("Profile", Icons.Default.Person, ProfileActivity::class.java)
        )

        navItems.forEach { (label, icon, targetActivity) ->
            val isSelected = activity::class.java == targetActivity
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        val intent = Intent(activity, targetActivity)
                        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        activity.startActivity(intent)
                    }
                },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = if (darkMode) Color.Black else Color.White,
                    unselectedIconColor = if (darkMode) Color.LightGray else Color.Gray,
                    selectedTextColor = if (darkMode) Color(0xFF81C784) else Color(0xFF2E7D32),
                    unselectedTextColor = if (darkMode) Color.LightGray else Color.Gray,
                    indicatorColor = if (darkMode) Color(0xFF81C784) else Color(0xFF2E7D32)
                )
            )
        }
    }
}
