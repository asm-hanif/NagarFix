package com.hanif.nagarfix

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.map

class RecoveryActivity : ComponentActivity() {
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
                RecoveryScreen(darkMode)
            }
        }
    }
}

@Composable
fun RecoveryScreen(isDarkMode: Boolean) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(50.dp)
                    .background(
                        color = Color(0xFF2E7D32),
                        shape = RoundedCornerShape(bottomEnd = 30.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Recover Account",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(80.dp))

            Text(
                text = "Password Recovery",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(30.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
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
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (email.isNotBlank()) {
                        auth.sendPasswordResetEmail(email)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "Reset link sent to $email", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32)),
                shape = RoundedCornerShape(30.dp)
            ) {
                Text(
                    text = "Send Reset Link",
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.Black else Color.White
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
