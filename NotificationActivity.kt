package com.hanif.nagarfix

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.google.firebase.database.*
import kotlinx.coroutines.flow.map

data class NPost(
    val id: String = "",
    val title: String = "",
    val location: String = ""
)

class NotificationActivity : ComponentActivity() {
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkMode by dataStore.data.map { it[DARK_MODE_KEY] ?: false }.collectAsState(initial = false)

            MaterialTheme(
                colorScheme = if (darkMode) darkColorScheme(
                    primary = Color(0xFF81C784),
                    onPrimary = Color.Black,
                    surface = Color(0xFF1E1E1E),
                    onSurface = Color.White,
                    background = Color(0xFF121212),
                    onBackground = Color.White
                ) else lightColorScheme(
                    primary = Color(0xFF2E7D32),
                    onPrimary = Color.White,
                    surface = Color.White,
                    onSurface = Color.Black,
                    background = Color(0xFFF2F2F2),
                    onBackground = Color.Black
                )
            ) {
                NotificationScreen(this, darkMode)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(activity: Activity, isDarkMode: Boolean) {
    val nposts = remember { mutableStateListOf<NPost>() }
    var isLoading by remember { mutableStateOf(false) }

    val database = FirebaseDatabase.getInstance()
    val postsRef = database.getReference("posts")

    LaunchedEffect(Unit) {
        isLoading = true
        postsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = mutableListOf<NPost>()
                for (child in snapshot.children) {
                    try {
                        val id = child.key ?: continue
                        val title = child.child("title").getValue(String::class.java) ?: "No Title"
                        val location = child.child("location").getValue(String::class.java) ?: "Unknown Location"
                        tempList.add(NPost(id, title, location))
                    } catch (e: Exception) {
                        continue
                    }
                }
                tempList.sortByDescending { it.id }
                nposts.clear()
                nposts.addAll(tempList)
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
            }
        })
    }

    Scaffold(
        bottomBar = { BottomNavigationBar(activity) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = 0.6f)
                    .height(50.dp)
                    .background(
                        color = Color(0xFF2E7D32),
                        shape = RoundedCornerShape(bottomEnd = 30.dp)
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Reports",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isLoading && nposts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2E7D32))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(nposts, key = { it.id }) { post ->
                        NotificationItem(post, isDarkMode)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(post: NPost, isDarkMode: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = post.title, 
                fontWeight = FontWeight.Bold, 
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "📍 Location: ${post.location}", 
                fontSize = 14.sp,
                color = if (isDarkMode) Color.LightGray else Color.Gray
            )
        }
    }
}
