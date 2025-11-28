package com.example.arena.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.arena.Screen
import com.example.arena.model.User
import com.example.arena.ui.components.ArenaButton
import com.example.arena.ui.components.ArenaTextField
import com.example.arena.ui.theme.ArenaBlack
import com.example.arena.ui.theme.ArenaGreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun SignUpScreen(navController: NavController) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    // --- YANGI USERNI BAZAGA YOZISH ---
    fun saveUserToDb(uid: String) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)

        val newUser = User(
            uid = uid,
            username = username,
            email = email,
            marketValue = 100.00, // Start narxi
            coins = 50,           // Start bonusi
            status = "ROOKIE"
        )

        userRef.set(newUser)
            .addOnSuccessListener {
                isLoading = false
                // Muvaffaqiyatli bo'lsa -> Home ga o'tamiz
                navController.navigate(Screen.Home.route) { popUpTo(0) }
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "DB ERROR: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaBlack)
            .padding(24.dp)
    ) {
        // Burchak dekoratsiyasi

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("SIGNUP", color = ArenaGreen, fontSize = 24.sp, fontFamily = FontFamily.Monospace)
            Text("JOIN THE GRID", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

            Spacer(modifier = Modifier.height(10.dp))

            // INPUTLAR
            ArenaTextField(
                value = username,
                onValueChange = { username = it },
                label = "USERNAME (CALLSIGN)"
            )

            ArenaTextField(
                value = email,
                onValueChange = { email = it },
                label = "EMAIL_ADDRESS",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            ArenaTextField(
                value = password,
                onValueChange = { password = it },
                label = "PASSWORD",
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = "Toggle", tint = ArenaGreen)
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // TUGMA
            ArenaButton(
                text = "CREATE ACCOUNT",
                isLoading = isLoading,
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty() && username.isNotEmpty()) {
                        isLoading = true
                        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // Auth o'tdi, endi Bazaga yozamiz
                                    val uid = task.result.user?.uid
                                    if (uid != null) {
                                        saveUserToDb(uid)
                                    }
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, "ERROR: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        Toast.makeText(context, "FILL ALL FIELDS", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // LOGIN LINK
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("ALREADY HAVE AN ACCOUNT? ", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text("LOG IN", color = ArenaGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable { navController.navigate(Screen.Login.route) })
            }
        }
    }
}