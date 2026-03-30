package com.hanif.nagarfix

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.flow.map
import java.io.File

data class HomePost(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    var likes: Int = 0,
    var dislikes: Int = 0,
    val userId: String = "",
    val mediaPaths: List<String> = emptyList()
)

class HomeActivity : ComponentActivity() {
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    background = Color(0xFFF2F2F2),
                    onBackground = Color.Black,
                    onSurface = Color.Black
                )
            ) {
                HomeScreen(this, darkMode)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(activity: Activity, isDarkMode: Boolean) {
    val posts = remember { mutableStateListOf<HomePost>() }
    var isLoading by remember { mutableStateOf(false) }

    val database = FirebaseDatabase.getInstance()
    val postsRef = database.getReference("posts")

    val userLikes = remember { mutableStateMapOf<String, Boolean?>() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val editLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            fetchPostsOnce(postsRef, posts) { isLoading = it }
        }
    }

    LaunchedEffect(Unit) {
        fetchPostsOnce(postsRef, posts) { isLoading = it }
    }

    Scaffold(
        bottomBar = { BottomNavigationBar(activity) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(50.dp)
                    .background(
                        color = Color(0xFF2E7D32),
                        shape = RoundedCornerShape(bottomEnd = 30.dp)
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "NagarFix",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading && posts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2E7D32))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(posts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            userLikes = userLikes,
                            postsRef = postsRef,
                            onEdit = { p ->
                                val intent = Intent(activity, EditPostActivity::class.java).apply {
                                    putExtra("id", p.id)
                                    putExtra("title", p.title)
                                    putExtra("description", p.description)
                                    putExtra("location", p.location)
                                    putExtra("userId", p.userId)
                                }
                                editLauncher.launch(intent)
                            },
                            currentUserId = currentUserId,
                            isDarkMode = isDarkMode
                        )
                    }
                }
            }
        }
    }
}

private fun fetchPostsOnce(postsRef: DatabaseReference, outList: MutableList<HomePost>, setRefreshing: (Boolean) -> Unit = {}) {
    setRefreshing(true)
    postsRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val tempList = mutableListOf<HomePost>()
            for (child in snapshot.children) {
                try {
                    val id = child.key ?: continue
                    val title = child.child("title").getValue(String::class.java) ?: "No Title"
                    val description = child.child("description").getValue(String::class.java) ?: "No Description"
                    val location = child.child("location").getValue(String::class.java) ?: "Unknown Location"
                    val likes = child.child("likes").getValue(Long::class.java)?.toInt() ?: 0
                    val dislikes = child.child("dislikes").getValue(Long::class.java)?.toInt() ?: 0
                    val userId = child.child("userId").getValue(String::class.java) ?: ""
                    
                    val mediaPaths = mutableListOf<String>()
                    child.child("mediaPaths").children.forEach {
                        it.getValue(String::class.java)?.let { path -> mediaPaths.add(path) }
                    }

                    tempList.add(HomePost(id, title, description, location, likes, dislikes, userId, mediaPaths))
                } catch (_: Exception) { }
            }
            tempList.sortByDescending { it.id }
            outList.clear()
            outList.addAll(tempList)
            setRefreshing(false)
        }

        override fun onCancelled(error: DatabaseError) {
            setRefreshing(false)
        }
    })
}

@Composable
fun PostCard(
    post: HomePost,
    userLikes: MutableMap<String, Boolean?>,
    postsRef: DatabaseReference,
    onEdit: (HomePost) -> Unit,
    currentUserId: String,
    isDarkMode: Boolean
) {
    var likes by remember { mutableIntStateOf(post.likes) }
    var dislikes by remember { mutableIntStateOf(post.dislikes) }
    
    LaunchedEffect(post.likes, post.dislikes) {
        likes = post.likes
        dislikes = post.dislikes
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Location: ${post.location}",
                        fontSize = 14.sp,
                        color = if (isDarkMode) Color.LightGray else Color.Gray
                    )
                }
                if (post.userId == currentUserId) {
                    IconButton(onClick = { onEdit(post) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Post", tint = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = post.description, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))

            if (post.mediaPaths.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(post.mediaPaths) { path ->
                        val file = File(path)
                        val uri = Uri.fromFile(file)
                        val isVideo = path.endsWith(".mp4") || path.endsWith(".mkv") || path.endsWith(".3gp")

                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isVideo) {
                                Icon(Icons.Default.PlayCircle, contentDescription = "Video", tint = Color.White, modifier = Modifier.size(40.dp))
                            } else {
                                Image(
                                    painter = rememberAsyncImagePainter(uri),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val currentLikeState = userLikes[post.id]
                    if (currentLikeState == true) {
                        likes = (likes - 1).coerceAtLeast(0)
                        userLikes[post.id] = null
                    } else {
                        if (currentLikeState == false) {
                            dislikes = (dislikes - 1).coerceAtLeast(0)
                        }
                        likes += 1
                        userLikes[post.id] = true
                    }
                    postsRef.child(post.id).child("likes").setValue(likes)
                    postsRef.child(post.id).child("dislikes").setValue(dislikes)
                }) {
                    Icon(
                        Icons.Filled.ThumbUp,
                        contentDescription = "Like",
                        tint = if (userLikes[post.id] == true) (if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32)) else Color.Gray
                    )
                }
                Text(text = "$likes", color = MaterialTheme.colorScheme.onSurface)

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(onClick = {
                    val currentLikeState = userLikes[post.id]
                    if (currentLikeState == false) {
                        dislikes = (dislikes - 1).coerceAtLeast(0)
                        userLikes[post.id] = null
                    } else {
                        if (currentLikeState == true) {
                            likes = (likes - 1).coerceAtLeast(0)
                        }
                        dislikes += 1
                        userLikes[post.id] = false
                    }
                    postsRef.child(post.id).child("likes").setValue(likes)
                    postsRef.child(post.id).child("dislikes").setValue(dislikes)
                }) {
                    Icon(
                        Icons.Filled.ThumbDown,
                        contentDescription = "Dislike",
                        tint = if (userLikes[post.id] == false) Color.Red else Color.Gray
                    )
                }
                Text(text = "$dislikes", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
