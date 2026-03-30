package com.hanif.nagarfix

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File

class ProfileActivity : ComponentActivity() {

    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        setContent {
            val scope = rememberCoroutineScope()
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
                    background = Color(0xFFF5F5F5),
                    onBackground = Color.Black
                )
            ) {
                ProfileScreen(this, darkMode) {
                    scope.launch {
                        dataStore.edit { preferences ->
                            preferences[DARK_MODE_KEY] = !darkMode
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(activity: Activity, darkMode: Boolean, onToggleDarkMode: () -> Unit) {
    val context = LocalContext.current
    val deepGreenLight = if (darkMode) Color(0xFF81C784) else Color(0xFF2E7D32)
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    var userName by remember { mutableStateOf(currentUser?.displayName ?: "User") }
    var profileBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(currentUser?.uid) {
        // Load name from Firestore
        currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    userName = doc.getString("name") ?: currentUser.displayName ?: "User"
                }
            }
        }
        // Load local profile pic
        val file = File(context.filesDir, "profile_pic.jpg")
        if (file.exists()) {
            profileBitmap = BitmapFactory.decodeFile(file.absolutePath)
        }
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
            // Header with User Name beside Profile Image
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFF2E7D32),
                        shape = RoundedCornerShape(bottomEnd = 30.dp)
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (profileBitmap != null) {
                    Image(
                        bitmap = profileBitmap!!.asImageBitmap(),
                        contentDescription = "Profile Image",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.profile),
                        contentDescription = "Profile Image",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = userName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(30.dp))

            // Stats or Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(
                        text = "Account Information",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (darkMode) Color(0xFF81C784) else Color(0xFF2E7D32)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Email: ${currentUser?.email ?: ""}",
                        fontSize = 14.sp,
                        color = if (darkMode) Color.LightGray else Color.Gray
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            OptionCard(
                icon = Icons.Default.Edit,
                iconTint = deepGreenLight,
                text = "Edit Profile",
                onClick = { activity.startActivity(Intent(activity, EditProfileActivity::class.java)) }
            )

            OptionCard(
                icon = Icons.Default.DarkMode,
                iconTint = deepGreenLight,
                text = "Dark Mode",
                trailing = {
                    Switch(
                        checked = darkMode,
                        onCheckedChange = { onToggleDarkMode() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = deepGreenLight,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.LightGray
                        )
                    )
                }
            )

            OptionCard(
                icon = Icons.Default.ExitToApp,
                iconTint = Color.Red,
                text = "Logout",
                textColor = Color.Red,
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(activity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    activity.startActivity(intent)
                    activity.finish()
                }
            )
        }
    }
}

@Composable
fun OptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = text, tint = iconTint)
            Spacer(Modifier.width(16.dp))
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textColor)
            Spacer(Modifier.weight(1f))
            trailing?.invoke()
        }
    }
}
