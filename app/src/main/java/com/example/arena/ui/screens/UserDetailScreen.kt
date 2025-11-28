package com.example.arena.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.arena.Screen
import com.example.arena.model.Challenge
import com.example.arena.model.Notification
import com.example.arena.model.User
import com.example.arena.ui.components.ChallengeItem
import com.example.arena.ui.theme.ArenaBlack
import com.example.arena.ui.theme.ArenaGreen
import com.example.arena.ui.theme.ArenaRed
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun UserDetailScreen(navController: NavController, userId: String) {
    val context = LocalContext.current
    var targetUser by remember { mutableStateOf<User?>(null) }
    var targetChallenges by remember { mutableStateOf<List<Challenge>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Tab holati (0 = Active Bets, 1 = History, 2 = Achievements)
    var selectedTab by remember { mutableIntStateOf(0) }

    var myCoins by remember { mutableStateOf(0) }
    var isFollowing by remember { mutableStateOf(false) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val currentUserName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Gladiator"

    LaunchedEffect(userId) {
        try {
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("users").document(userId).get().await()
            targetUser = doc.toObject(User::class.java)

            if (currentUserId != null) {
                val myDoc = db.collection("users").document(currentUserId).get().await()
                myCoins = myDoc.getLong("coins")?.toInt() ?: 0
                val followDoc = db.collection("users").document(userId)
                    .collection("followers").document(currentUserId).get().await()
                isFollowing = followDoc.exists()
            }

            // Barcha Challengelarni yuklaymiz (keyin filtrlaymiz)
            val challengesSnapshot = db.collection("challenges")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            targetChallenges = challengesSnapshot.toObjects(Challenge::class.java)
                .sortedByDescending { it.startTime }

            isLoading = false
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleFollow() {
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()
        val myRef = db.collection("users").document(currentUserId!!)
        val targetRef = db.collection("users").document(userId)

        if (isFollowing) {
            batch.delete(targetRef.collection("followers").document(currentUserId))
            batch.delete(myRef.collection("following").document(userId))
            batch.update(targetRef, "followersCount", FieldValue.increment(-1))
            batch.update(myRef, "followingCount", FieldValue.increment(-1))

            // Unfollow Notification
            val notifRef = db.collection("notifications").document()
            batch.set(notifRef, Notification(
                id = notifRef.id, userId = userId, senderId = currentUserId,
                senderName = currentUserName, type = "UNFOLLOW", amount = 0.0, timestamp = System.currentTimeMillis()
            ))

            isFollowing = false
            targetUser = targetUser?.copy(followersCount = targetUser!!.followersCount - 1)
            Toast.makeText(context, "Unfollowed", Toast.LENGTH_SHORT).show()
        } else {
            batch.set(targetRef.collection("followers").document(currentUserId), mapOf("uid" to currentUserId))
            batch.set(myRef.collection("following").document(userId), mapOf("uid" to userId))
            batch.update(targetRef, "followersCount", FieldValue.increment(1))
            batch.update(myRef, "followingCount", FieldValue.increment(1))

            // Follow Notification
            val notifRef = db.collection("notifications").document()
            batch.set(notifRef, Notification(
                id = notifRef.id, userId = userId, senderId = currentUserId,
                senderName = currentUserName, type = "FOLLOW", amount = 0.0, timestamp = System.currentTimeMillis()
            ))

            isFollowing = true
            targetUser = targetUser?.copy(followersCount = targetUser!!.followersCount + 1)
            Toast.makeText(context, "Following", Toast.LENGTH_SHORT).show()
        }
        batch.commit()
    }

    fun invest() {
        if (myCoins < 50) {
            Toast.makeText(context, "Insufficient funds", Toast.LENGTH_SHORT).show()
            return
        }
        // ... (Invest logic qoladi) ...
        Toast.makeText(context, "Invested $50", Toast.LENGTH_SHORT).show()
    }

    Box(modifier = Modifier.fillMaxSize().background(ArenaBlack)) {
        // Orqa fon gradienti (Rasmdagidek tepa qism yashilroq)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF003300), ArenaBlack)
                    )
                )
        )

        if (isLoading) {
            CircularProgressIndicator(color = ArenaGreen, modifier = Modifier.align(Alignment.Center))
        } else if (targetUser != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // HEADER (Back & Options)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    // O'rtadagi ism (Rasmdagidek: alex_vision)
                    Text(
                        text = targetUser!!.username.lowercase().replace(" ", "_"),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    IconButton(onClick = { /* Options */ }) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = Color.White)
                    }
                }

                // PROFILE INFO
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.Start // Chapga taqalgan (Rasmda shunday)
                ) {
                    // Avatar va Ism yonma-yon
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val avatarUrl = "https://api.dicebear.com/9.x/notionists/png?seed=${targetUser!!.uid}&backgroundColor=FFD700" // Rasmdagi sariq rang

                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF222222)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = targetUser!!.username,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "@${targetUser!!.username.lowercase().replace(" ", "_")}",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // BIO
                    Text(
                        text = "Building the future, one bet at a time. Focused on tech and sustainable energy markets.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // BUTTONS (Follow & Invest)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Follow Button (Kulrang)
                        Button(
                            onClick = { toggleFollow() },
                            modifier = Modifier.weight(1f).height(45.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                            shape = RoundedCornerShape(30.dp)
                        ) {
                            Text(if (isFollowing) "Following" else "Follow", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }

                        // Invest Button (Yashil)
                        Button(
                            onClick = { invest() },
                            modifier = Modifier.weight(1f).height(45.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ArenaGreen),
                            shape = RoundedCornerShape(30.dp)
                        ) {
                            Text("Invest", color = ArenaBlack, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // STATS ROW (Followers, Following, Investments)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem("1.2K", "Followers")
                        StatItem("450", "Following")
                        StatItem("312", "Investments")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // INFO CARDS (Win Rate & Reliability)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InfoCard(label = "Win Rate", value = "82%", modifier = Modifier.weight(1f))
                        InfoCard(label = "Reliability Score", value = "9.5/10", modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // --- TABS (Active Bets / History / Achievements) ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    TabItem("Active Bets", selectedTab == 0) { selectedTab = 0 }
                    TabItem("History", selectedTab == 1) { selectedTab = 1 }
                    TabItem("Achievements", selectedTab == 2) { selectedTab = 2 }
                }

                // Yashil chiziq (Tab Indicator)
                // Hisoblash qiyin bo'lmasligi uchun sodda chiziq chizamiz
                Divider(color = Color(0xFF222222), thickness = 1.dp)

                Spacer(modifier = Modifier.height(20.dp))

                // --- CONTENT ---
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
                                .height(200.dp)
                                .border(1.dp, Color(0xFF222222), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Content for ${if(selectedTab==0) "Active Bets" else "History"}", color = Color.Gray)
                        }
                    } else {
                        filteredChallenges.forEach { challenge ->
                            // Bu yerda ChallengeItem yoki HistoryCard ishlatamiz
                            // Rasmdagi dizayn uchun oddiyroq qilib qo'yamiz
                            ChallengeItem(challenge, {}, isReadOnly = true)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

// --- YORDAMCHI UI KOMPONENTLAR ---

@Composable
fun StatItem(value: String, label: String) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun InfoCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
            color = if (isSelected) Color.White else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
        if (isSelected) {
            // Tagidagi yashil chiziq (shartli ravishda)
            // Aslida Box bilan qilish kerak, bu yerda oddiy qilib matn rangini o'zgartirdik
        }
    }
}