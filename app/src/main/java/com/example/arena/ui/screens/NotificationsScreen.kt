package com.example.arena.ui.screens

import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.arena.R
import com.example.arena.Screen
import com.example.arena.model.Notification
import com.example.arena.ui.theme.ArenaBlack
import com.example.arena.ui.theme.ArenaGreen
import com.example.arena.ui.theme.ArenaRed
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun NotificationsScreen(navController: NavController) {
    val context = LocalContext.current
    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    // --- LOGIKA (O'ZGARISHSIZ) ---
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

    // --- UI ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaBlack)
    ) {
        // 1. Orqa fon gradienti
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF003300),
                            ArenaBlack
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // 2. HEADER
            Spacer(modifier = Modifier.height(20.dp)) // Status bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }

            Text(
                text = stringResource(R.string.notifications_title), // <--- TARJIMA
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // 3. RO'YXAT
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                if (notifications.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.no_new_activity), // <--- TARJIMA
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 20.dp)
                        )
                    }
                } else {
                    items(notifications) { notif ->
                        NotificationItemNew(
                            notif = notif,
                            onClick = {
                                if (notif.senderId.isNotEmpty()) {
                                    navController.navigate(Screen.UserDetail.createRoute(notif.senderId))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItemNew(notif: Notification, onClick: () -> Unit) {
    val timeAgo = DateUtils.getRelativeTimeSpanString(
        notif.timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()

    val (icon, iconColor) = when (notif.type) {
        "INVEST" -> Pair(Icons.Rounded.TrendingUp, ArenaGreen)
        "FOLLOW" -> Pair(Icons.Rounded.PersonAdd, ArenaGreen)
        "VOTE_VALID" -> Pair(Icons.Rounded.CheckCircle, ArenaGreen)
        "VOTE_FAKE" -> Pair(Icons.Rounded.Cancel, ArenaRed)
        "UNFOLLOW" -> Pair(Icons.Rounded.PersonRemove, ArenaRed)
        else -> Pair(Icons.Rounded.Notifications, Color.Gray)
    }

    // --- MATNLARNI TARJIMA QILISH ---
    val titleText = when (notif.type) {
        "INVEST" -> "@${notif.senderName} ${stringResource(R.string.notif_invested)}"
        "FOLLOW" -> "@${notif.senderName} ${stringResource(R.string.notif_followed)}"
        "VOTE_VALID" -> stringResource(R.string.notif_bet_won)
        "VOTE_FAKE" -> stringResource(R.string.notif_bet_lost)
        "UNFOLLOW" -> "@${notif.senderName} ${stringResource(R.string.notif_unfollowed)}"
        else -> stringResource(R.string.notif_default)
    }

    val subText = when (notif.type) {
        "INVEST" -> stringResource(R.string.desc_invest)
        "FOLLOW" -> stringResource(R.string.desc_follow)
        "VOTE_VALID" -> "${stringResource(R.string.desc_win)} +${notif.amount}" // Dinamik qism qo'shildi
        "VOTE_FAKE" -> stringResource(R.string.desc_loss)
        "UNFOLLOW" -> stringResource(R.string.desc_unfollow)
        else -> stringResource(R.string.desc_default)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Yashil nuqta
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(ArenaGreen, Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Ikonka Konteyneri
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF111111)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Matnlar
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titleText,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subText,
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Vaqt
        Text(
            text = timeAgo,
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}