package com.hanif.nagarfix

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircle
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
import androidx.core.app.ActivityCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileOutputStream

class EditPostActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val postId = intent.getStringExtra("id") ?: ""
        val initialTitle = intent.getStringExtra("title") ?: ""
        val initialDescription = intent.getStringExtra("description") ?: ""
        val initialLocation = intent.getStringExtra("location") ?: ""

        setContent {
            val darkMode by dataStore.data.map { it[DARK_MODE_KEY] ?: false }.collectAsState(initial = false)

            MaterialTheme(
                colorScheme = if (darkMode) darkColorScheme(
                    primary = Color(0xFF81C784),
                    surface = Color(0xFF1E1E1E),
                    background = Color(0xFF121212),
                    onBackground = Color.White,
                    onSurface = Color.White
                ) else lightColorScheme(
                    primary = Color(0xFF2E7D32),
                    surface = Color.White,
                    background = Color.White,
                    onBackground = Color.Black,
                    onSurface = Color.Black
                )
            ) {
                EditPostScreen(
                    activity = this,
                    postId = postId,
                    initialTitle = initialTitle,
                    initialDescription = initialDescription,
                    initialLocation = initialLocation,
                    fusedLocationClient = fusedLocationClient,
                    isDarkMode = darkMode
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPostScreen(
    activity: Activity,
    postId: String,
    initialTitle: String,
    initialDescription: String,
    initialLocation: String,
    fusedLocationClient: FusedLocationProviderClient,
    isDarkMode: Boolean
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
    var location by remember { mutableStateOf(initialLocation) }
    
    val context = LocalContext.current
    val database = FirebaseDatabase.getInstance()
    val postsRef = database.getReference("posts")
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    var mediaItems by remember { mutableStateOf<List<Any>>(emptyList()) }
    var isMediaLoaded by remember { mutableStateOf(false) }

    if (!isMediaLoaded && postId.isNotEmpty()) {
        LaunchedEffect(postId) {
            postsRef.child(postId).child("mediaPaths").get().addOnSuccessListener { snapshot ->
                val paths = mutableListOf<String>()
                snapshot.children.forEach { child ->
                    child.getValue(String::class.java)?.let { paths.add(it) }
                }
                mediaItems = paths
                isMediaLoaded = true
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        mediaItems = mediaItems + uris
    }

    Scaffold(
        topBar = {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    IconButton(onClick = { activity.finish() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = "Edit Post",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            EditPostTextField(value = title, onValueChange = { title = it }, label = "Title", isDarkMode = isDarkMode)
            Spacer(modifier = Modifier.height(8.dp))

            EditPostTextField(value = description, onValueChange = { description = it }, label = "Description", isDarkMode = isDarkMode)
            Spacer(modifier = Modifier.height(8.dp))

            EditPostTextField(value = location, onValueChange = { location = it }, label = "Location", isDarkMode = isDarkMode)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        Toast.makeText(activity, "Please enable GPS", Toast.LENGTH_SHORT).show()
                        activity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    } else {
                        if (ActivityCompat.checkSelfPermission(
                                activity,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            fusedLocationClient.getCurrentLocation(
                                Priority.PRIORITY_HIGH_ACCURACY,
                                CancellationTokenSource().token
                            ).addOnSuccessListener { loc ->
                                loc?.let {
                                    location = "${it.latitude}, ${it.longitude}"
                                }
                            }
                        } else {
                            ActivityCompat.requestPermissions(
                                activity,
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                1
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(50.dp)
                    .shadow(8.dp, RoundedCornerShape(30.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(30.dp)
            ) {
                Text("Use Current Location", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { galleryLauncher.launch("*/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("Change Images/Videos", color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            mediaItems.forEachIndexed { index, item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    val uri = if (item is Uri) item else Uri.fromFile(File(item as String))
                    val path = if (item is String) item else item.toString()
                    val isVideo = path.endsWith(".mp4") || path.endsWith(".mkv") || path.endsWith(".3gp") || 
                                 (item is Uri && context.contentResolver.getType(item)?.contains("video") == true)

                    if (isVideo) {
                        Icon(Icons.Default.PlayCircle, contentDescription = "Video", tint = Color.White, modifier = Modifier.size(50.dp))
                    } else {
                        Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    IconButton(
                        onClick = { mediaItems = mediaItems.toMutableList().apply { removeAt(index) } },
                        modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(bottomStart = 8.dp))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (postId.isNotEmpty()) {
                        postsRef.child(postId).get().addOnSuccessListener { snapshot ->
                            val postOwnerId = snapshot.child("userId").getValue(String::class.java)
                            if (postOwnerId == currentUserId) {
                                
                                val finalMediaPaths = mediaItems.mapNotNull { item ->
                                    if (item is String) item
                                    else if (item is Uri) saveMediaLocally(context, item)
                                    else null
                                }

                                val updates = mapOf(
                                    "title" to title,
                                    "description" to description,
                                    "location" to location,
                                    "mediaPaths" to finalMediaPaths
                                )
                                
                                postsRef.child(postId).updateChildren(updates).addOnSuccessListener {
                                    Toast.makeText(activity, "Post updated", Toast.LENGTH_SHORT).show()
                                    activity.setResult(Activity.RESULT_OK)
                                    activity.finish()
                                }.addOnFailureListener {
                                    Toast.makeText(activity, "Failed to update post", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(activity, "You can only edit your own posts", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(50.dp)
                    .shadow(8.dp, RoundedCornerShape(30.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(30.dp)
            ) {
                Text("Save Changes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (postId.isNotEmpty()) {
                        postsRef.child(postId).get().addOnSuccessListener { snapshot ->
                            val postOwnerId = snapshot.child("userId").getValue(String::class.java)
                            if (postOwnerId == currentUserId) {
                                postsRef.child(postId).removeValue().addOnSuccessListener {
                                    Toast.makeText(activity, "Post deleted", Toast.LENGTH_SHORT).show()
                                    activity.setResult(Activity.RESULT_OK)
                                    activity.finish()
                                }.addOnFailureListener {
                                    Toast.makeText(activity, "Failed to delete post", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(activity, "You can only delete your own posts", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(50.dp)
                    .shadow(8.dp, RoundedCornerShape(30.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape = RoundedCornerShape(30.dp)
            ) {
                Text("Delete Post", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun saveMediaLocally(context: Context, uri: Uri): String? {
    return try {
        val extension = context.contentResolver.getType(uri)?.split("/")?.lastOrNull() ?: "bin"
        val fileName = "media_${System.currentTimeMillis()}_${(0..1000).random()}.$extension"
        val mediaDir = File(context.filesDir, "posts_media")
        if (!mediaDir.exists()) mediaDir.mkdirs()
        
        val file = File(mediaDir, fileName)
        
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}

@Composable
fun EditPostTextField(value: String, onValueChange: (String) -> Unit, label: String, isDarkMode: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        shape = RoundedCornerShape(30.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFE0E0E0),
            unfocusedContainerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFE0E0E0),
            focusedBorderColor = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32),
            unfocusedBorderColor = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32),
            focusedTextColor = if (isDarkMode) Color.White else Color.Black,
            unfocusedTextColor = if (isDarkMode) Color.White else Color.Black,
            focusedLabelColor = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32),
            unfocusedLabelColor = if (isDarkMode) Color.Gray else Color.DarkGray
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        singleLine = true
    )
}
