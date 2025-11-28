package com.example.arena.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.arena.Screen
import com.example.arena.ui.theme.ArenaBlack
import com.example.arena.ui.theme.ArenaGreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {

    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }
    var terminalText by remember { mutableStateOf("> SYSTEM BOOT...") }

    val view = LocalView.current

    LaunchedEffect(key1 = true) {
        // 1. Animatsiya (o'sha-o'sha)
        alpha.animateTo(1f, animationSpec = tween(1000))

        repeat(2) {
            scale.animateTo(1.2f, animationSpec = tween(100))
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            scale.animateTo(1.0f, animationSpec = tween(100))
            delay(100)
        }

        delay(500)
        terminalText = "> CHECKING BIOMETRICS..."
        delay(600)

        // --- O'ZGARISH SHU YERDA ---
        // Firebase'dan so'raymiz: "User bormi?"
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            // AGAR USER BO'LSA -> HOME GA OTAMIZ
            terminalText = "> WELCOME BACK, ${currentUser.displayName ?: "GLADIATOR"}."
            delay(500) // User yozuvni o'qishi uchun ozgina kutamiz
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        } else {
            // AGAR USER YO'Q BO'LSA -> LOGIN GA OTAMIZ
            terminalText = "> ACCESS RESTRICTED."
            delay(500)
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaBlack)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = "Arena Logo",
                tint = ArenaGreen,
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale.value)
                    .alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = terminalText,
                color = ArenaGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.alpha(alpha.value)
            )
        }

        Text(
            text = "V.2.0 // AUTO_LOGIN",
            color = Color.DarkGray,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(0.5f)
        )
    }
}