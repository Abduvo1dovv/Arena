package com.example.arena.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    // --- BAZANI TEKSHIRISH FUNKSIYASI ---
    fun checkAndCreateUserInDb() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(user.uid)

            userRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    navController.navigate(Screen.Home.route) { popUpTo(0) }
                } else {
                    val newUser = User(
                        uid = user.uid,
                        username = user.displayName ?: "GLADIATOR",
                        email = user.email ?: "",
                        marketValue = 100.00,
                        coins = 50,
                        status = "ROOKIE"
                    )
                    userRef.set(newUser).addOnSuccessListener {
                        navController.navigate(Screen.Home.route) { popUpTo(0) }
                    }
                }
            }
        }
    }

    // --- GOOGLE SIGN IN ---
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            isLoading = true
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.result
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            checkAndCreateUserInDb()
                        } else {
                            isLoading = false
                            errorMessage = "GOOGLE LOGIN FAILED"
                        }
                    }
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "ERROR: ${e.message}"
            }
        }
    }

    fun startGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            // SIZNING WEB CLIENT ID INGIZ:
            .requestIdToken("1026629804794-04bh3f9lmcpb0m3779li4aesv06t1rhf.apps.googleusercontent.com")
            .requestEmail()
            .build()

        val googleClient = GoogleSignIn.getClient(context, gso)

        googleClient.signOut().addOnCompleteListener {
            val signInIntent = googleClient.signInIntent
            googleLauncher.launch(signInIntent)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaBlack)
            .padding(24.dp)
    ) {
        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("STATUS: SECURE", color = ArenaGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("NODE: 74.125.224.72", color = ArenaGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("SYSTEM LOGIN", color = ArenaGreen, fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text("AUTHENTICATION REQUIRED", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)

            Spacer(modifier = Modifier.height(10.dp))

            ArenaTextField(
                value = email,
                onValueChange = { email = it },
                label = "CODENAME (EMAIL)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            ArenaTextField(
                value = password,
                onValueChange = { password = it },
                label = "PASSCODE",
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = "Toggle", tint = ArenaGreen)
                    }
                }
            )

            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = Color.Red, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }

            // PASSWORD LOGIN BUTTON
            ArenaButton(
                text = "ACCESS",
                isOutline = true,
                isLoading = isLoading,
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        isLoading = true
                        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    checkAndCreateUserInDb()
                                } else {
                                    isLoading = false
                                    errorMessage = "ACCESS DENIED"
                                }
                            }
                    }
                }
            )

            Text("- OR -", color = Color.Gray, fontSize = 10.sp)

            // GOOGLE BUTTON (Yashil ramkali, qora fon)
            Button(
                onClick = { startGoogleSignIn() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                border = BorderStroke(1.dp, ArenaGreen), // YASHIL CHIZIQ
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(50)
            ) {
                // Rangli Google yozuvi
                Text("G", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("o", color = Color(0xFFFFA500), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("o", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("g", color = Color.Blue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("l", color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("e", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("CONTINUE WITH GOOGLE", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // SIGNUP LINK
        Text(
            text = "NO ID? JOIN THE GRID ->",
            color = Color.Gray,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .clickable { navController.navigate(Screen.Signup.route) }
        )
    }
}