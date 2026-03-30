package com.hanif.nagarfix

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Auto-login check
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContent {
            val darkMode by dataStore.data.map { it[DARK_MODE_KEY] ?: false }.collectAsState(initial = false)

            MaterialTheme(
                colorScheme = if (darkMode) darkColorScheme(
                    primary = Color(0xFF81C784),
                    background = Color(0xFF121212),
                    onBackground = Color.White
                ) else lightColorScheme(
                    primary = Color(0xFF2E7D32),
                    background = Color.White,
                    onBackground = Color.Black
                )
            ) {
                MainPage(
                    onGetStartedClick = {
                        startActivity(Intent(this, LoginActivity::class.java))
                    },
                    onAdminLoginClick = {
                        startActivity(Intent(this, AdminLoginActivity::class.java))
                    },
                    isDarkMode = darkMode
                )
            }
        }
    }
}

@Composable
fun MainPage(
    onGetStartedClick: () -> Unit,
    onAdminLoginClick: () -> Unit,
    isDarkMode: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = if (isDarkMode) 0.3f else 1f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Logo and Name
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.nagarfixlogo),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "NagarFix",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color(0xFF81C784) else Color(0xFF006400)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Text(
                    text = "Fix The City",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkMode) Color(0xFF81C784) else Color(0xFF006400)
                )
                Text(
                    text = "Together",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkMode) Color(0xFF81C784) else Color(0xFF006400)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            Spacer(modifier = Modifier.weight(1f))

            // Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onGetStartedClick,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(50.dp)
                        .shadow(8.dp, RoundedCornerShape(30.dp))
                ) {
                    Text(
                        text = "Get Started",
                        color = if (isDarkMode) Color.Black else Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(onClick = onAdminLoginClick) {
                    Text(
                        text = "Admin Login",
                        color = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
