package com.hanif.nagarfix

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Photo
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
import androidx.datastore.preferences.core.booleanPreferencesKey
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.database.*
import kotlinx.coroutines.flow.map

data class AdminPost(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val status: String = "Pending"
)

class AdminViewActivity : ComponentActivity() {
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
                AdminViewScreen(this, darkMode)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminViewScreen(activity: Activity, isDarkMode: Boolean) {
    val posts = remember { mutableStateListOf<AdminPost>() }
    var isLoading by remember { mutableStateOf(false) }
    val database = FirebaseDatabase.getInstance()
    val postsRef = database.getReference("posts")

    LaunchedEffect(Unit) {
        isLoading = true
        postsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = mutableListOf<AdminPost>()
                for (child in snapshot.children) {
                    val id = child.key ?: ""
                    val title = child.child("title").getValue(String::class.java) ?: ""
                    val description = child.child("description").getValue(String::class.java) ?: ""
                    val location = child.child("location").getValue(String::class.java) ?: ""
                    val status = child.child("status").getValue(String::class.java) ?: "Pending"
                    tempList.add(AdminPost(id, title, description, location, status))
                }
                posts.clear()
                posts.addAll(tempList.reversed())
                isLoading = false
            }
            override fun onCancelled(error: DatabaseError) {
                isLoading = false
            }
        })
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { _ ->
        Toast.makeText(activity, "Photo captured for validation", Toast.LENGTH_SHORT).show()
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { _ ->
        Toast.makeText(activity, "Photo selected from gallery", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel", color = Color.White) },
                actions = {
                    IconButton(onClick = {
                        activity.startActivity(Intent(activity, MainActivity::class.java))
                        activity.finish()
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2E7D32))
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading && posts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF2E7D32))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(posts, key = { it.id }) { post ->
                    AdminPostCard(post, onMarkAsFixed = {
                        postsRef.child(post.id).child("status").setValue("Fixed")
                            .addOnSuccessListener {
                                Toast.makeText(activity, "Post marked as fixed", Toast.LENGTH_SHORT).show()
                            }
                    }, onPickMedia = { isCamera ->
                        if (isCamera) cameraLauncher.launch(null)
                        else galleryLauncher.launch("image/*")
                    }, isDarkMode = isDarkMode)
                }
            }
        }
    }
}

@Composable
fun AdminPostCard(post: AdminPost, onMarkAsFixed: () -> Unit, onPickMedia: (Boolean) -> Unit, isDarkMode: Boolean) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = post.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = post.status,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (post.status == "Fixed") Color(0xFF2E7D32) else Color.Red
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(text = post.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            Spacer(Modifier.height(4.dp))
            Text(text = "📍 ${post.location}", fontSize = 14.sp, color = if (isDarkMode) Color.LightGray else Color.Gray)
            Spacer(Modifier.height(12.dp))

            if (post.status != "Fixed") {
                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Text("Resolve Issue", color = if (isDarkMode) Color.Black else Color.White)
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Upload Proof") },
            text = { Text("Take a photo or pick from gallery to confirm the fix.") },
            confirmButton = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onPickMedia(true); showDialog = false }, 
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32))
                    ) {
                        Icon(Icons.Default.CameraAlt, "", tint = if (isDarkMode) Color.Black else Color.White)
                        Text(" Camera", color = if (isDarkMode) Color.Black else Color.White)
                    }
                    Button(
                        onClick = { onPickMedia(false); showDialog = false }, 
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32))
                    ) {
                        Icon(Icons.Default.Photo, "", tint = if (isDarkMode) Color.Black else Color.White)
                        Text(" Gallery", color = if (isDarkMode) Color.Black else Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { onMarkAsFixed(); showDialog = false }) {
                    Text("Skip & Mark Fixed", color = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32))
                }
            }
        )
    }
}
