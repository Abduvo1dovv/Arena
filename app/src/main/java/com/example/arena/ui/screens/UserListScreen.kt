package com.example.arena.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.arena.model.Notification
import com.example.arena.model.User
import com.example.arena.ui.theme.ArenaBlack
import com.example.arena.ui.theme.ArenaGreen
import com.example.arena.ui.theme.ArenaRed
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class UserListItem(
    val user: User,
    var isFollowing: Boolean = false,
    var isMe: Boolean = false
)

@Composable
fun UserListScreen(navController: NavController, userId: String, listType: String) {
    val context = LocalContext.current
    var usersList by remember { mutableStateOf<List<UserListItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isMyProfile = currentUserId == userId

    // TITLE TARJIMASI
    val title =
        if (listType == "followers") stringResource(R.string.followers_title) else stringResource(R.string.following_title)

    // Stringlar (Toast uchun)
    val unfollowedMsg =
        stringResource(R.string.unfollow) + "ed" // Oddiy yechim (yoki alohida string qo'shish kerak)
    val removedMsg = "User removed" // Buni ham qo'shish mumkin
    val followingMsg = stringResource(R.string.btn_following)

    // LOGIKA
    fun handleAction(item: UserListItem) {
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()
        val targetId = item.user.uid
        val myId = currentUserId ?: return

        if (isMyProfile) {
            if (listType == "following") {
                batch.delete(
                    db.collection("users").document(myId).collection("following").document(targetId)
                )
                batch.delete(
                    db.collection("users").document(targetId).collection("followers").document(myId)
                )
                batch.update(
                    db.collection("users").document(myId),
                    "followingCount",
                    FieldValue.increment(-1)
                )
                batch.update(
                    db.collection("users").document(targetId),
                    "followersCount",
                    FieldValue.increment(-1)
                )
                usersList = usersList.filter { it.user.uid != targetId }
                Toast.makeText(context, "Unfollowed", Toast.LENGTH_SHORT).show()
            } else {
                batch.delete(
                    db.collection("users").document(myId).collection("followers").document(targetId)
                )
                batch.delete(
                    db.collection("users").document(targetId).collection("following").document(myId)
                )
                batch.update(
                    db.collection("users").document(myId),
                    "followersCount",
                    FieldValue.increment(-1)
                )
                batch.update(
                    db.collection("users").document(targetId),
                    "followingCount",
                    FieldValue.increment(-1)
                )
                usersList = usersList.filter { it.user.uid != targetId }
                Toast.makeText(context, "Removed", Toast.LENGTH_SHORT).show()
            }
        } else {
            val myRef = db.collection("users").document(myId)
            val targetRef = db.collection("users").document(targetId)

            if (item.isFollowing) {
                batch.delete(targetRef.collection("followers").document(myId))
                batch.delete(myRef.collection("following").document(targetId))
                batch.update(targetRef, "followersCount", FieldValue.increment(-1))
                batch.update(myRef, "followingCount", FieldValue.increment(-1))
                usersList =
                    usersList.map { if (it.user.uid == targetId) it.copy(isFollowing = false) else it }
                Toast.makeText(context, "Unfollowed", Toast.LENGTH_SHORT).show()
            } else {
                batch.set(targetRef.collection("followers").document(myId), mapOf("uid" to myId))
                batch.set(
                    myRef.collection("following").document(targetId),
                    mapOf("uid" to targetId)
                )
                batch.update(targetRef, "followersCount", FieldValue.increment(1))
                batch.update(myRef, "followingCount", FieldValue.increment(1))

                val notifRef = db.collection("notifications").document()
                val notif = Notification(
                    id = notifRef.id,
                    userId = targetId,
                    senderId = myId,
                    senderName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Gladiator",
                    type = "FOLLOW",
                    amount = 0.0,
                    timestamp = System.currentTimeMillis()
                )
                batch.set(notifRef, notif)

                usersList =
                    usersList.map { if (it.user.uid == targetId) it.copy(isFollowing = true) else it }
                Toast.makeText(context, "Following", Toast.LENGTH_SHORT).show()
            }
        }
        batch.commit()
    }

    LaunchedEffect(Unit) {
        if (currentUserId != null) {
            try {
                val db = FirebaseFirestore.getInstance()
                val myFollowingIds =
                    db.collection("users").document(currentUserId).collection("following").get()
                        .await().documents.map { it.id }.toSet()
                val targetListIds =
                    db.collection("users").document(userId).collection(listType).get()
                        .await().documents.map { it.id }

                val fetchedList = mutableListOf<UserListItem>()
                for (id in targetListIds) {
                    val u =
                        db.collection("users").document(id).get().await().toObject(User::class.java)
                    if (u != null) {
                        fetchedList.add(
                            UserListItem(
                                u,
                                myFollowingIds.contains(id),
                                id == currentUserId
                            )
                        )
                    }
                }
                usersList = fetchedList
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
            }
        }
    }

    // UI
    Box(modifier = Modifier
        .fillMaxSize()
        .background(ArenaBlack)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Brush.verticalGradient(colors = listOf(Color(0xFF003300), ArenaBlack)))
        )

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(bottom = 20.dp)
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = ArenaGreen) }
            } else if (usersList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.list_empty),
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    ) // <--- TARJIMA
                }
            } else {
                LazyColumn {
                    items(usersList) { item ->
                        UserItemRowNew(
                            item = item,
                            isMyProfileList = isMyProfile,
                            listType = listType,
                            onActionClick = { handleAction(item) },
                            onProfileClick = {
                                navController.navigate(
                                    Screen.UserDetail.createRoute(
                                        item.user.uid
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserItemRowNew(
    item: UserListItem,
    isMyProfileList: Boolean,
    listType: String,
    onActionClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val showButton = !item.isMe
    val avatarUrl =
        "https://api.dicebear.com/9.x/notionists/png?seed=${item.user.uid}&backgroundColor=00ff41"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
            .clickable { onProfileClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF222222)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = item.user.username,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Val: $${item.user.marketValue.toInt()}",
                        color = ArenaGreen,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (showButton) {
                // TUGMA MATNI VA RANGI (TARJIMA QILINGAN)
                val (btnText, btnColor) = when {
                    isMyProfileList && listType == "followers" -> Pair(
                        stringResource(R.string.remove),
                        ArenaRed
                    ) // <--- TARJIMA
                    isMyProfileList && listType == "following" -> Pair(
                        stringResource(R.string.unfollow),
                        Color.Gray
                    ) // <--- TARJIMA
                    item.isFollowing -> Pair(
                        stringResource(R.string.btn_following),
                        Color.Gray
                    ) // <--- TARJIMA
                    else -> Pair(stringResource(R.string.follow), ArenaGreen) // <--- TARJIMA
                }

                val containerColor = if (btnColor == ArenaGreen) ArenaGreen else Color.Transparent
                val contentColor = if (btnColor == ArenaGreen) ArenaBlack else Color.White
                val borderColor = if (btnColor == ArenaGreen) ArenaGreen else btnColor

                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(containerColor = containerColor),
                    border = if (containerColor == Color.Transparent) androidx.compose.foundation.BorderStroke(
                        1.dp,
                        borderColor
                    ) else null,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = btnText,
                        color = contentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}