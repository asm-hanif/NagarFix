package com.hanif.nagarfix

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.map

class SignUpActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

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
                SignUpScreen(
                    onSignUpClick = { name, email, password, confirmPassword ->
                        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                            return@SignUpScreen
                        }

                        if (password != confirmPassword) {
                            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                            return@SignUpScreen
                        }

                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val userId = auth.currentUser?.uid ?: ""
                                    val userMap = hashMapOf(
                                        "name" to name,
                                        "email" to email
                                    )

                                    firestore.collection("users")
                                        .document(userId)
                                        .set(userMap)
                                        .addOnSuccessListener {
                                            val intent = Intent(this, HomeActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                            finish()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(this, "Error saving data: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                } else {
                                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    },
                    onLoginClick = {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    },
                    isDarkMode = darkMode
                )
            }
        }
    }
}

@Composable
fun SignUpScreen(
    onSignUpClick: (name: String, email: String, password: String, confirmPassword: String) -> Unit,
    onLoginClick: () -> Unit,
    isDarkMode: Boolean
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

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
                    text = "Join The Fix",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "Sign Up",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(30.dp))

            SignUpTextField(value = name, onValueChange = { name = it }, label = "Name", isDarkMode = isDarkMode)
            Spacer(modifier = Modifier.height(12.dp))
            SignUpTextField(value = email, onValueChange = { email = it }, label = "Email", keyboardType = KeyboardType.Email, isDarkMode = isDarkMode)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                shape = RoundedCornerShape(30.dp),
                colors = signUpTextFieldColors(isDarkMode),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = "Toggle password",
                            tint = if (isDarkMode) Color.LightGray else Color.Gray
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                shape = RoundedCornerShape(30.dp),
                colors = signUpTextFieldColors(isDarkMode),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = "Toggle password",
                            tint = if (isDarkMode) Color.LightGray else Color.Gray
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onSignUpClick(name, email, password, confirmPassword) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(50.dp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(30.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32)),
                shape = RoundedCornerShape(30.dp)
            ) {
                Text(
                    text = "Sign Up",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.Black else Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already have an account? ", color = if (isDarkMode) Color.LightGray else Color.Gray)
                Text(
                    text = "Login",
                    color = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onLoginClick() }
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun SignUpTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isDarkMode: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        shape = RoundedCornerShape(30.dp),
        colors = signUpTextFieldColors(isDarkMode),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

@Composable
fun signUpTextFieldColors(isDarkMode: Boolean) = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFE0E0E0),
    unfocusedContainerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFE0E0E0),
    focusedBorderColor = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32),
    unfocusedBorderColor = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32),
    focusedTextColor = if (isDarkMode) Color.White else Color.Black,
    unfocusedTextColor = if (isDarkMode) Color.White else Color.Black,
    focusedLabelColor = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32),
    unfocusedLabelColor = if (isDarkMode) Color.Gray else Color.DarkGray
)
