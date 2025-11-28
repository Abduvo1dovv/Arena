package com.example.arena.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.arena.Screen
import com.example.arena.model.Notification
import com.example.arena.ui.theme.ArenaBlack
import com.example.arena.ui.theme.ArenaGreen
import com.example.arena.ui.theme.ArenaRed
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationsScreen(navController: NavController) {
    val context = LocalContext.current
    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(Unit) {
        if (uid != null) {
            try {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("notifications")
                    .whereEqualTo("userId", uid)
                    .get().await()

                val rawList = snapshot.toObjects(Notification::class.java)
                notifications = rawList.sortedByDescending { it.timestamp }

                val batch = db.batch()
                var hasUpdates = false
                snapshot.documents.forEach { doc ->
                    if (doc.getBoolean("isRead") == false) {
                        batch.update(doc.reference, "isRead", true)
                        hasUpdates = true
                    }
                }
                if (hasUpdates) batch.commit()

            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(ArenaBlack).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Gray, modifier = Modifier.clickable { navController.popBackStack() })
            Spacer(modifier = Modifier.width(10.dp))
            Text("ACTIVITY LOG", color = ArenaGreen, fontSize = 20.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(20.dp))
        LazyColumn {
            items(notifications) { notif ->
                NotificationItem(
                    notif = notif,
                    onClick = {
                        if (notif.senderId.isNotEmpty()) {
                            navController.navigate(Screen.UserDetail.createRoute(notif.senderId))
                        }
                    }
                )
            }
        }
        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("NO ACTIVITY YET", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun NotificationItem(notif: Notification, onClick: () -> Unit) {
    val date = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(notif.timestamp))

    // --- LOGIKA ---
    val icon = when (notif.type) {
        "INVEST" -> Icons.Rounded.TrendingUp
        "UNFOLLOW" -> Icons.Rounded.PersonRemove // Qizil ikonka
        else -> Icons.Rounded.GroupAdd
    }

    val titleColor = when (notif.type) {
        "INVEST" -> ArenaGreen
        "UNFOLLOW" -> ArenaRed // Qizil rang
        else -> Color(0xFF007AFF)
    }

    val titleText = when (notif.type) {
        "INVEST" -> "${notif.senderName.uppercase()} INVESTED IN YOU"
        "UNFOLLOW" -> "${notif.senderName.uppercase()} LOST FAITH" // Qattiqroq gap
        else -> "${notif.senderName.uppercase()} STARTED FOLLOWING"
    }

    val subText = when (notif.type) {
        "INVEST" -> "Market Value increased by $${notif.amount}"
        "UNFOLLOW" -> "They unfollowed you. Prove them wrong!"
        else -> "Check their profile to follow back"
    }

    val borderColor = if (notif.type == "UNFOLLOW") ArenaRed.copy(alpha = 0.3f) else Color(0xFF222222)

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, borderColor, RoundedCornerShape(12.dp)).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = titleColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = titleText, color = if(notif.type == "UNFOLLOW") ArenaRed else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = subText, color = Color.Gray, fontSize = 12.sp)
            }
            Text(text = date, color = Color.DarkGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}