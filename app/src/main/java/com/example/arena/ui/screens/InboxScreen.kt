package com.example.arena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.arena.Screen
import com.example.arena.model.ChatRoom
import com.example.arena.model.User
import com.example.arena.ui.theme.ArenaBlack
import com.example.arena.ui.theme.ArenaGreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class ChatItemData(
    val chatRoom: ChatRoom,
    val otherUser: User?
)

@Composable
fun InboxScreen(navController: NavController) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val scope = rememberCoroutineScope()

    var chatList by remember { mutableStateOf<List<ChatItemData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // JONLI KUZATUV
    DisposableEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        if (currentUserId == null) return@DisposableEffect onDispose { }

        val listener = db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val rooms = snapshot.toObjects(ChatRoom::class.java)
                        .sortedByDescending { it.lastMessageTime }

                    scope.launch {
                        val fullList = mutableListOf<ChatItemData>()
                        for (room in rooms) {
                            val otherUserId = room.participants.find { it != currentUserId }
                            if (otherUserId != null) {
                                try {
                                    val userDoc = db.collection("users").document(otherUserId).get().await()
                                    val otherUser = userDoc.toObject(User::class.java)
                                    fullList.add(ChatItemData(room, otherUser))
                                } catch (ex: Exception) {
                                    fullList.add(ChatItemData(room, null))
                                }
                            }
                        }
                        chatList = fullList
                        isLoading = false
                    }
                } else {
                    isLoading = false
                }
            }
        onDispose { listener.remove() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaBlack)
    ) {
        // 1. LIST (Orqa fonda)
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ArenaGreen)
            }
        } else if (chatList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("NO MESSAGES YET", color = Color.Gray, fontFamily = FontFamily.Monospace)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // Header balandligi (taxminan 80dp + status bar)
                contentPadding = PaddingValues(top = 100.dp, bottom = 80.dp)
            ) {
                items(chatList) { item ->
                    InboxItem(item, currentUserId!!) {
                        if (item.otherUser != null) {
                            navController.navigate(com.example.arena.Screen.Chat.createRoute(item.otherUser.uid))
                        }
                    }
                }
            }
        }

        // 2. ODDIY HEADER (Tepada qotib turadi)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF003300), Color.Transparent) // Yengil gradient
                    )
                )
                .zIndex(1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding() // Soatdan pastga tushadi
                    .height(56.dp) // Standart Toolbar balandligi
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ORQAGA TUGMASI (Doirasiz, oddiy)
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(24.dp) // Kichikroq va ixcham
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // SARLAVHA
                Text(
                    text = "DIRECT MESSAGES",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 3. FAB (Pastda)
        FloatingActionButton(
            onClick = { navController.navigate(com.example.arena.Screen.Search.route) },
            containerColor = ArenaGreen,
            contentColor = ArenaBlack,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .zIndex(1f)
        ) {
            Icon(Icons.Default.AddComment, contentDescription = "New Chat")
        }
    }
}

@Composable
fun InboxItem(item: ChatItemData, myId: String, onClick: () -> Unit) {
    val time = if (item.chatRoom.lastMessageTime > 0) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.chatRoom.lastMessageTime))
    } else ""

    val isUnread = (item.chatRoom.lastSenderId != myId) && !item.chatRoom.isRead
    val textColor = if (isUnread) Color.White else Color.Gray
    val fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFF222222))
                .border(if (isUnread) 2.dp else 1.dp, ArenaGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.otherUser?.username?.take(1)?.uppercase() ?: "?",
                color = ArenaGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = item.otherUser?.username ?: "Unknown",
                    color = Color.White,
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(text = time, color = if (isUnread) ArenaGreen else Color.Gray, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.chatRoom.lastMessage,
                    color = textColor,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = fontWeight,
                    modifier = Modifier.weight(1f)
                )

                if (isUnread) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(10.dp).background(ArenaGreen, CircleShape))
                }
            }
        }
    }
    Divider(color = Color(0xFF222222), thickness = 1.dp, modifier = Modifier.padding(start = 88.dp))
}