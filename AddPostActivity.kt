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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.datastore.preferences.core.booleanPreferencesKey
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileOutputStream

class AddPostActivity : ComponentActivity() {
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
                    background = Color.White,
                    onBackground = Color.Black
                )
            ) {
                AddPostScreen(this, darkMode)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPostScreen(activity: Activity, isDarkMode: Boolean) {
    var senderName by remember { mutableStateOf("") }
    var senderEmail by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedMediaUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Map picker launcher
    val mapPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val name = result.data?.getStringExtra("location_name")
            if (!name.isNullOrEmpty()) {
                location = name
            }
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) fetchCurrentLocation(context, fusedLocationClient) { location = it }
        else Toast.makeText(context, "Location permission denied!", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(Unit) {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            fetchCurrentLocation(context, fusedLocationClient) { location = it }
        }
    }

    // Gallery launcher for both images and videos
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        selectedMediaUris = selectedMediaUris + uris
    }

    Scaffold(
        bottomBar = { BottomNavigationBar(activity) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {

            // Header
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
                    text = "Create a Post",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            AddPostTextField(value = senderName, onValueChange = { senderName = it }, label = "Your Name", isDarkMode)
            Spacer(Modifier.height(10.dp))
            AddPostTextField(value = senderEmail, onValueChange = { senderEmail = it }, label = "Your Email", isDarkMode)
            Spacer(Modifier.height(10.dp))
            AddPostTextField(value = title, onValueChange = { title = it }, label = "Title", isDarkMode)
            Spacer(Modifier.height(10.dp))
            AddPostTextField(value = description, onValueChange = { description = it }, label = "Description", isDarkMode)
            Spacer(Modifier.height(10.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier.weight(1f),
                    colors = textFieldColors(isDarkMode)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { mapPickerLauncher.launch(Intent(activity, MapPickerActivity::class.java)) },
                    modifier = Modifier.background(Color(0xFF2E7D32), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Map, contentDescription = "Pick on map", tint = Color.White)
                }
            }

            Spacer(Modifier.height(25.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Button(
                    onClick = { galleryLauncher.launch("*/*") }, // Allow all media types
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Icon(Icons.Default.PermMedia, contentDescription = "Gallery", tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Images/Videos", color = Color.White)
                }
            }

            Spacer(Modifier.height(10.dp))

            selectedMediaUris.forEach { uri ->
                val isVideo = context.contentResolver.getType(uri)?.startsWith("video") == true
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
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
                        onClick = { selectedMediaUris = selectedMediaUris.toMutableList().apply { remove(uri) } },
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(23.dp))

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = Color(0xFF2E7D32))
            }

            Button(
                onClick = {
                    if (senderName.isNotBlank() && senderEmail.isNotBlank() && title.isNotBlank() &&
                        description.isNotBlank() && location.isNotBlank()
                    ) {
                        isLoading = true
                        try {
                            val database = FirebaseDatabase.getInstance()
                            val postsRef = database.getReference("posts")
                            val postId = postsRef.push().key ?: System.currentTimeMillis().toString()
                            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

                            // Save media locally and get their paths
                            val localMediaPaths = selectedMediaUris.mapNotNull { uri ->
                                saveMediaLocally(context, uri)
                            }

                            val postMap = hashMapOf<String, Any>(
                                "id" to postId,
                                "senderName" to senderName,
                                "senderEmail" to senderEmail,
                                "title" to title,
                                "description" to description,
                                "location" to location,
                                "userId" to currentUserId,
                                "mediaPaths" to localMediaPaths,
                                "likes" to 0,
                                "dislikes" to 0,
                                "timestamp" to System.currentTimeMillis()
                            )

                            postsRef.child(postId).setValue(postMap)
                                .addOnSuccessListener {
                                    isLoading = false
                                    Toast.makeText(activity, "Your issue posted successfully!", Toast.LENGTH_LONG).show()
                                    val intent = Intent(activity, HomeActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                    activity.startActivity(intent)
                                    activity.finish()
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    Toast.makeText(activity, "Failed to post: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        } catch (e: Exception) {
                            isLoading = false
                            Toast.makeText(activity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(activity, "Please fill all fields!", Toast.LENGTH_SHORT).show()
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
                Text("Post Issue", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(Modifier.height(30.dp))
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
fun AddPostTextField(value: String, onValueChange: (String) -> Unit, label: String, isDarkMode: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        shape = RoundedCornerShape(30.dp),
        colors = textFieldColors(isDarkMode),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        singleLine = true
    )
}

@Composable
fun textFieldColors(isDarkMode: Boolean) = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFE0E0E0),
    unfocusedContainerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFE0E0E0),
    focusedBorderColor = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32),
    unfocusedBorderColor = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32),
    focusedTextColor = if (isDarkMode) Color.White else Color.Black,
    unfocusedTextColor = if (isDarkMode) Color.White else Color.Black,
    focusedLabelColor = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32),
    unfocusedLabelColor = if (isDarkMode) Color.Gray else Color.DarkGray
)

private fun fetchCurrentLocation(
    context: android.content.Context,
    fusedLocationClient: FusedLocationProviderClient,
    onLocationFetched: (String) -> Unit
) {
    val locationManager = context.getSystemService(Activity.LOCATION_SERVICE) as LocationManager
    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        return
    }

    if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { loc ->
            if (loc != null) {
                try {
                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                    val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                    if (addresses?.isNotEmpty() == true) {
                        onLocationFetched(addresses[0].getAddressLine(0))
                    } else {
                        onLocationFetched("${loc.latitude}, ${loc.longitude}")
                    }
                } catch (e: Exception) {
                    onLocationFetched("${loc.latitude}, ${loc.longitude}")
                }
            }
        }
    }
}
