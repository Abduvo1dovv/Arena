package com.example.arena.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.arena.Screen
import com.example.arena.model.Notification
import com.example.arena.model.User
import com.example.arena.ui.theme.ArenaBlack
import com.example.arena.ui.theme.ArenaGreen
import com.example.arena.ui.theme.ArenaRed
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Yordamchi model
data class UserListItem(
    val user: User,
    var isFollowing: Boolean = false, // Men unga obuna bo'lganmanmi?
    var isMe: Boolean = false // Bu menmi?
)

@Composable
fun UserListScreen(navController: NavController, userId: String, listType: String) {
    val context = LocalContext.current
    var usersList by remember { mutableStateOf<List<UserListItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isMyProfile = currentUserId == userId // Bu mening ro'yxatimmi?

    val title = if (listType == "followers") "FOLLOWERS" else "FOLLOWING"

    // --- FOLLOW / UNFOLLOW / REMOVE LOGIKASI ---
    fun handleAction(item: UserListItem) {
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()
        val targetId = item.user.uid
        val myId = currentUserId ?: return

        // 1. HOLAT: Men o'z "Following" ro'yxatimdaman va UNFOLLOW qilyapman
        if (isMyProfile && listType == "following") {
            val myRef = db.collection("users").document(myId)
            val targetRef = db.collection("users").document(targetId)

            batch.delete(myRef.collection("following").document(targetId))
            batch.delete(targetRef.collection("followers").document(myId))

            batch.update(myRef, "followingCount", FieldValue.increment(-1))
            batch.update(targetRef, "followersCount", FieldValue.increment(-1))

            // Ro'yxatdan olib tashlaymiz
            usersList = usersList.filter { it.user.uid != targetId }
            batch.commit()
            Toast.makeText(context, "Unfollowed", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. HOLAT: Men o'z "Followers" ro'yxatimdaman va REMOVE qilyapman
        if (isMyProfile && listType == "followers") {
            val myRef = db.collection("users").document(myId)
            val targetRef = db.collection("users").document(targetId)

            batch.delete(myRef.collection("followers").document(targetId))
            batch.delete(targetRef.collection("following").document(myId))

            batch.update(myRef, "followersCount", FieldValue.increment(-1))
            batch.update(targetRef, "followingCount", FieldValue.increment(-1))

            usersList = usersList.filter { it.user.uid != targetId }
            batch.commit()
            Toast.makeText(context, "Removed follower", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. HOLAT: Men birovning ro'yxatidaman va kimgadir FOLLOW/UNFOLLOW qilyapman
        val myRef = db.collection("users").document(myId)
        val targetRef = db.collection("users").document(targetId)

        if (item.isFollowing) {
            // Unfollow
            batch.delete(targetRef.collection("followers").document(myId))
            batch.delete(myRef.collection("following").document(targetId))
            batch.update(targetRef, "followersCount", FieldValue.increment(-1))
            batch.update(myRef, "followingCount", FieldValue.increment(-1))

            usersList = usersList.map { if (it.user.uid == targetId) it.copy(isFollowing = false) else it }
            Toast.makeText(context, "Unfollowed", Toast.LENGTH_SHORT).show()
        } else {
            // Follow
            val data = mapOf("uid" to myId, "timestamp" to System.currentTimeMillis())
            batch.set(targetRef.collection("followers").document(myId), data)
            batch.set(myRef.collection("following").document(targetId), mapOf("uid" to targetId))

            batch.update(targetRef, "followersCount", FieldValue.increment(1))
            batch.update(myRef, "followingCount", FieldValue.increment(1))

            // Notification
            val notifRef = db.collection("notifications").document()
            val notif = Notification(
                id = notifRef.id, userId = targetId, senderId = myId,
                senderName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Gladiator",
                type = "FOLLOW", amount = 0.0, timestamp = System.currentTimeMillis()
            )
            batch.set(notifRef, notif)

            usersList = usersList.map { if (it.user.uid == targetId) it.copy(isFollowing = true) else it }
            Toast.makeText(context, "Following", Toast.LENGTH_SHORT).show()
        }
        batch.commit()
    }

    // RO'YXATNI YUKLASH
    LaunchedEffect(Unit) {
        if (currentUserId != null) {
            try {
                val db = FirebaseFirestore.getInstance()

                // 1. Mening "Following" ro'yxatim (Solishtirish uchun)
                val myFollowingSnapshot = db.collection("users").document(currentUserId).collection("following").get().await()
                val myFollowingIds = myFollowingSnapshot.documents.map { it.id }.toSet()

                // 2. Ko'rsatilayotgan ro'yxat ID lari
                val snapshot = db.collection("users").document(userId).collection(listType).get().await()
                val userIds = snapshot.documents.map { it.id }

                val fetchedList = mutableListOf<UserListItem>()
                for (id in userIds) {
                    val userDoc = db.collection("users").document(id).get().await()
                    val userObj = userDoc.toObject(User::class.java)
                    if (userObj != null) {
                        fetchedList.add(
                            UserListItem(
                                user = userObj,
                                isFollowing = myFollowingIds.contains(id),
                                isMe = (id == currentUserId)
                            )
                        )
                    }
                }
                usersList = fetchedList
                isLoading = false
            } catch (e: Exception) { isLoading = false }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(ArenaBlack).padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Gray, modifier = Modifier.clickable { navController.popBackStack() })
            Spacer(modifier = Modifier.width(10.dp))
            Text(title, color = ArenaGreen, fontSize = 18.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(20.dp))

        if (isLoading) {
            CircularProgressIndicator(color = ArenaGreen, modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (usersList.isEmpty()) {
            Text("NO USERS FOUND", color = Color.DarkGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        } else {
            LazyColumn {
                items(usersList) { item ->
                    UserItemRow(
                        item = item,
                        isMyProfileList = isMyProfile,
                        listType = listType,
                        onActionClick = { handleAction(item) },
                        onProfileClick = { navController.navigate(Screen.UserDetail.createRoute(item.user.uid)) }
                    )
                }
            }
        }
    }
}

@Composable
fun UserItemRow(
    item: UserListItem,
    isMyProfileList: Boolean,
    listType: String,
    onActionClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    // O'zimizni ko'rsatayotganda tugma kerak emas
    val showButton = !item.isMe

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).border(1.dp, Color(0xFF222222), RoundedCornerShape(12.dp)).clickable { onProfileClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF222222)), contentAlignment = Alignment.Center) {
                    Text(text = item.user.username.take(1).uppercase(), color = ArenaGreen, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(item.user.username, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Val: $${item.user.marketValue.toInt()}", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }

            if (showButton) {
                // Tugma matni va rangi
                var btnText = "FOLLOW"
                var btnColor = Color(0xFF007AFF) // Ko'k
                var btnContainer = Color.Transparent
                var btnBorder = Color.Gray

                if (isMyProfileList && listType == "followers") {
                    btnText = "REMOVE"
                    btnColor = ArenaRed
                    btnBorder = ArenaRed
                } else if (isMyProfileList && listType == "following") {
                    btnText = "UNFOLLOW"
                    btnColor = Color.Gray
                } else if (item.isFollowing) {
                    btnText = "FOLLOWING"
                    btnColor = Color.Gray
                } else {
                    // Default: Follow (Ko'k va to'liq fon)
                    btnContainer = Color(0xFF007AFF)
                    btnColor = Color.White
                    btnBorder = Color.Transparent
                }

                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(containerColor = btnContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, btnBorder),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(btnText, color = btnColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}