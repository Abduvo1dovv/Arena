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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.arena.R
import com.example.arena.Screen
import com.example.arena.model.Challenge
import com.example.arena.model.Notification
import com.example.arena.model.User
import com.example.arena.ui.components.ChallengeItem
import com.example.arena.ui.theme.ArenaBlack
import com.example.arena.ui.theme.ArenaGreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun UserDetailScreen(navController: NavController, userId: String) {
    val context = LocalContext.current

    // STATE
    var targetUser by remember { mutableStateOf<User?>(null) }
    var targetChallenges by remember { mutableStateOf<List<Challenge>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Active, 1=History

    // User Data
    var myCoins by remember { mutableStateOf(0) }
    var isFollowing by remember { mutableStateOf(false) }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val currentUserName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Gladiator"
    val investmentAmount = 50.0

    // MA'LUMOTLARNI YUKLASH
    LaunchedEffect(userId) {
        try {
            val db = FirebaseFirestore.getInstance()

            // Target User
            val doc = db.collection("users").document(userId).get().await()
            targetUser = doc.toObject(User::class.java)

            // Current User & Follow Status
            if (currentUserId != null) {
                val myDoc = db.collection("users").document(currentUserId).get().await()
                myCoins = myDoc.getLong("coins")?.toInt() ?: 0

                val followDoc = db.collection("users").document(userId)
                    .collection("followers").document(currentUserId).get().await()
                isFollowing = followDoc.exists()
            }

            // Challenges
            val challengesSnapshot = db.collection("challenges")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            targetChallenges = challengesSnapshot.toObjects(Challenge::class.java)
                .sortedByDescending { it.startTime }

            isLoading = false
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            isLoading = false
        }
    }

    // --- FOLLOW LOGIKASI ---
    fun toggleFollow() {
        if (currentUserId == null) return
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()
        val myRef = db.collection("users").document(currentUserId)
        val targetRef = db.collection("users").document(userId)

        if (isFollowing) {
            // UNFOLLOW
            batch.delete(targetRef.collection("followers").document(currentUserId))
            batch.delete(myRef.collection("following").document(userId))
            batch.update(targetRef, "followersCount", FieldValue.increment(-1))
            batch.update(myRef, "followingCount", FieldValue.increment(-1))

            val notifRef = db.collection("notifications").document()
            batch.set(
                notifRef, Notification(
                    id = notifRef.id,
                    userId = userId,
                    senderId = currentUserId,
                    senderName = currentUserName,
                    type = "UNFOLLOW",
                    amount = 0.0,
                    timestamp = System.currentTimeMillis()
                )
            )

            isFollowing = false
            targetUser = targetUser?.copy(followersCount = targetUser!!.followersCount - 1)
            Toast.makeText(context, "Unfollowed", Toast.LENGTH_SHORT).show()
        } else {
            // FOLLOW
            batch.set(
                targetRef.collection("followers").document(currentUserId),
                mapOf("uid" to currentUserId)
            )
            batch.set(myRef.collection("following").document(userId), mapOf("uid" to userId))
            batch.update(targetRef, "followersCount", FieldValue.increment(1))
            batch.update(myRef, "followingCount", FieldValue.increment(1))

            val notifRef = db.collection("notifications").document()
            batch.set(
                notifRef, Notification(
                    id = notifRef.id,
                    userId = userId,
                    senderId = currentUserId,
                    senderName = currentUserName,
                    type = "FOLLOW",
                    amount = 0.0,
                    timestamp = System.currentTimeMillis()
                )
            )

            isFollowing = true
            targetUser = targetUser?.copy(followersCount = targetUser!!.followersCount + 1)
            Toast.makeText(context, "Following", Toast.LENGTH_SHORT).show()
        }
        batch.commit()
    }

    // --- INVEST LOGIKASI ---
    fun invest() {
        if (currentUserId == null) return
        if (myCoins < investmentAmount) {
            Toast.makeText(context, "INSUFFICIENT FUNDS! Need 50 Coins", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()
        val myRef = db.collection("users").document(currentUserId)
        val targetRef = db.collection("users").document(userId)

        // 1. Mendan pul yechish
        batch.update(myRef, "coins", myCoins - investmentAmount.toInt())
        // 2. Target narxini oshirish
        batch.update(targetRef, "marketValue", FieldValue.increment(10.0))

        // 3. Notification
        val notifRef = db.collection("notifications").document()
        batch.set(
            notifRef, Notification(
                id = notifRef.id,
                userId = userId,
                senderId = currentUserId,
                senderName = currentUserName,
                type = "INVEST",
                amount = 10.0,
                timestamp = System.currentTimeMillis()
            )
        )

        batch.commit().addOnSuccessListener {
            myCoins -= investmentAmount.toInt()
            targetUser = targetUser?.copy(marketValue = targetUser!!.marketValue + 10.0)
            Toast.makeText(context, "INVESTED 50 COINS!", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(ArenaBlack)) {
        // Orqa fon gradienti
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Brush.verticalGradient(colors = listOf(Color(0xFF003300), ArenaBlack)))
        )

        if (isLoading) {
            CircularProgressIndicator(
                color = ArenaGreen,
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (targetUser != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // HEADER
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = targetUser!!.username.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    IconButton(onClick = { /* Options */ }) {
                        Icon(
                            Icons.Default.MoreHoriz,
                            contentDescription = "More",
                            tint = Color.White
                        )
                    }
                }

                // PROFILE INFO
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    // Avatar va Ism
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val avatarUrl =
                            "https://api.dicebear.com/9.x/notionists/png?seed=${targetUser!!.uid}&backgroundColor=00ff41"
                        AsyncImage(
                            model = avatarUrl, contentDescription = "Avatar",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF222222)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                targetUser!!.username,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            // MARKET VALUE (O'ZGARTIRILDI: CAPITAL / REPUTATION SO'ZIGA MOSLASH)
                            Text(
                                "${stringResource(R.string.capital)}: $${targetUser!!.marketValue}",
                                color = ArenaGreen,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ) // <--- TARJIMA
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Gladiator in the Arena. Focused on winning and high stakes challenges.",
                        color = Color.Gray, fontSize = 14.sp, lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- ACTION BUTTONS ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // FOLLOW / UNFOLLOW
                        Button(
                            onClick = { toggleFollow() },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(
                                imageVector = if (isFollowing) Icons.Rounded.PersonRemove else Icons.Rounded.PersonAdd,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // TARJIMA
                            Text(
                                text = if (isFollowing) stringResource(R.string.unfollow) else stringResource(
                                    R.string.follow
                                ), // <--- TARJIMA
                                color = Color.White, fontWeight = FontWeight.SemiBold
                            )
                        }

                        // MESSAGE
                        Button(
                            onClick = { navController.navigate(Screen.Chat.createRoute(userId)) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(
                                Icons.Rounded.ChatBubbleOutline,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // TARJIMA
                            Text(
                                stringResource(R.string.message),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            ) // <--- TARJIMA
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // INVEST BUTTON
                    Button(
                        onClick = { invest() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ArenaGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        // TARJIMA: INVEST $50
                        Text(
                            "${stringResource(R.string.invest)} $50 🚀",
                            color = ArenaBlack,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ) // <--- TARJIMA
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // STATS ROW
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // TARJIMA: FOLLOWERS / FOLLOWING / WIN RATE
                        StatItem(
                            formatStatNumber(targetUser!!.followersCount),
                            stringResource(R.string.followers)
                        ) { // <--- TARJIMA
                            navController.navigate(Screen.UserList.createRoute(userId, "followers"))
                        }

                        StatItem(
                            formatStatNumber(targetUser!!.followingCount),
                            stringResource(R.string.following)
                        ) { // <--- TARJIMA
                            navController.navigate(Screen.UserList.createRoute(userId, "following"))
                        }

                        StatItem("0%", stringResource(R.string.win_rate)) {} // <--- TARJIMA
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // TABS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    TabItem(stringResource(R.string.active_bets), selectedTab == 0) {
                        selectedTab = 0
                    } // <--- TARJIMA
                    TabItem(stringResource(R.string.history), selectedTab == 1) {
                        selectedTab = 1
                    } // <--- TARJIMA
                }
                Divider(color = Color(0xFF222222), thickness = 1.dp)
                Spacer(modifier = Modifier.height(20.dp))

                // CONTENT LIST
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    val filteredChallenges = when (selectedTab) {
                        0 -> targetChallenges.filter { it.status == "ACTIVE" || it.status == "PENDING" }
                        1 -> targetChallenges.filter { it.status == "COMPLETED" || it.status == "FAILED" }
                        else -> emptyList()
                    }

                    if (filteredChallenges.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .border(1.dp, Color(0xFF222222), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            // TARJIMA: No data available
                            Text(
                                stringResource(R.string.no_cases),
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            ) // <--- TARJIMA (No cases to judge - ma'nosiga yaqin)
                        }
                    } else {
                        filteredChallenges.forEach { challenge ->
                            ChallengeItem(challenge, {}, isReadOnly = true)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

// YORDAMCHILAR
@Composable
fun StatItem(value: String, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
    }
}

// FUNKSIYA NOMI O'ZGARTIRILGAN (formatStatNumber)
private fun formatStatNumber(count: Int): String {
    return when {
        count >= 1000000 -> String.format("%.1fM", count / 1000000.0)
        count >= 1000 -> String.format("%.1fK", count / 1000.0)
        else -> count.toString()
    }
}

@Composable
fun TabItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(bottom = 12.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) ArenaGreen else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}