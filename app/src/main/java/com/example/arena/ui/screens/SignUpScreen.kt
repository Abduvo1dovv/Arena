package com.example.arena.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.arena.Screen
import com.example.arena.model.User
import com.example.arena.ui.theme.ArenaBlack
import com.example.arena.ui.theme.ArenaGreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(navController: NavController) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }


    fun saveUserToDb(uid: String) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)

        val newUser = User(
            uid = uid,
            username = username,
            email = email,
            marketValue = 0.0,
            coins = 50,
            status = "ROOKIE"
        )

        userRef.set(newUser)
            .addOnSuccessListener {
                isLoading = false
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
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF004400), Color.Transparent)
                    )
                )
                .align(Alignment.TopCenter)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "JOIN THE GRID",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))


            Column(modifier = Modifier.fillMaxWidth()) {

                TextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = { Text("Username", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF111111),
                        unfocusedContainerColor = Color(0xFF111111),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = ArenaGreen,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))


            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Email", color = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF111111),
                        unfocusedContainerColor = Color(0xFF111111),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = ArenaGreen,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))


            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Password", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF111111),
                        unfocusedContainerColor = Color(0xFF111111),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = ArenaGreen,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = "Toggle",
                                tint = Color.Gray
                            )
                        }
                    },
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(40.dp))


            Button(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty() && username.isNotEmpty()) {
                        isLoading = true
                        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val uid = task.result.user?.uid
                                    if (uid != null) {
                                        saveUserToDb(uid)
                                    }
                                } else {
                                    isLoading = false
                                    Toast.makeText(
                                        context,
                                        "ERROR: ${task.exception?.localizedMessage}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                    } else {
                        Toast.makeText(context, "FILL ALL FIELDS", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ArenaGreen),
                shape = RoundedCornerShape(50)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = ArenaBlack, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        "CREATE ACCOUNT",
                        color = ArenaBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))


            Row {
                Text("Already have an account? ", color = Color.Gray, fontSize = 12.sp)
                Text(
                    "Log In",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { navController.navigate(Screen.Login.route) }
                )
            }


            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "By creating an account, you agree to our Terms of Service and Privacy Policy.",
                color = Color.Gray,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}