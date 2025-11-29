package com.example.arena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.arena.Screen
import com.example.arena.model.User
import com.example.arena.ui.theme.ArenaBlack
import com.example.arena.ui.theme.ArenaGreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

@Composable
fun LeaderboardScreen(navController: NavController) {
    var leaders by remember { mutableStateOf<List<User>>(emptyList()) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid


    LaunchedEffect(Unit) {
        try {
            val snapshot = FirebaseFirestore.getInstance().collection("users")
                .orderBy("marketValue", Query.Direction.DESCENDING)
                .limit(50)
                .get().await()

            leaders = snapshot.toObjects(User::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaBlack)
            .padding(24.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD700))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "GLOBAL RANKING",
                color = Color.White,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            "WHO DOMINATES THE ARENA?",
            color = Color.Gray,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(20.dp))


        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(leaders) { index, user ->
                LeaderItem(
                    user = user,
                    rank = index + 1,
                    isMe = user.uid == currentUserId,

                    onClick = { navController.navigate(Screen.UserDetail.createRoute(user.uid)) }
                )
            }


            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun LeaderItem(user: User, rank: Int, isMe: Boolean, onClick: () -> Unit) {

    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> ArenaGreen
    }

    val backgroundColor = if (isMe) ArenaGreen.copy(alpha = 0.1f) else Color(0xFF111111)
    val borderColor = if (isMe) ArenaGreen else Color(0xFF222222)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "#$rank",
                color = rankColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(40.dp)
            )


            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF222222)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.username.take(1).uppercase(),
                    color = rankColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))


            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isMe) "${user.username} (YOU)" else user.username,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "$${String.format("%.2f", user.marketValue)}",
                    color = rankColor,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }


            if (rank <= 3) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = rankColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}